import unittest

from langchain_core.documents import Document
import requests

import app.services.ollama_service as ollama_module
from app.config import settings
from app.constants import (
    INSUFFICIENT_DATA_MESSAGE as CANONICAL_INSUFFICIENT_MESSAGE,
    OFF_TOPIC_MESSAGE,
)
from app.prompts.system_prompt import GENERAL_AGRICULTURE_PROMPT, RAG_PROMPT_TEMPLATE, SYSTEM_PROMPT
from app.services.rag_retrieval import (
    INSUFFICIENT_DATA_MESSAGE,
    RetrievedContext,
    detect_intent,
    expand_query,
    normalize_content_hash,
    is_insufficient_answer,
    select_best_contexts,
)
from app.services.rag_service import RagService
from app.services.ollama_service import OllamaService
from app.services.question_router import QuestionRouter, normalize_question
from app.services.source_sanitizer import (
    MAX_PUBLIC_SNIPPET_CHARS,
    sanitize_prompt_content,
    sanitize_public_snippet,
)


class RagRetrievalTests(unittest.TestCase):
    def test_detect_intent_high_confidence_category(self):
        intent = detect_intent("VietGAP yeu cau nguon nuoc nhu the nao?")

        self.assertEqual(intent.category, "vietgap")
        self.assertEqual(intent.confidence, "high")

    def test_detect_intent_low_confidence_searches_all_categories(self):
        intent = detect_intent("Toi can hoi them thong tin")

        self.assertIsNone(intent.category)
        self.assertEqual(intent.confidence, "low")

    def test_expand_query_returns_original_plus_max_three_expansions(self):
        queries = expand_query("ma QR ton kho thuoc sau phun thuoc")

        self.assertEqual(queries[0], "ma QR ton kho thuoc sau phun thuoc")
        self.assertLessEqual(len(queries), 4)
        self.assertIn("truy xuat nguon goc", " ".join(queries))
        self.assertIn("nhap kho", " ".join(queries))

    def test_select_best_contexts_filters_chroma_distance_and_deduplicates(self):
        doc_a = Document(
            page_content="Noi dung QR truy xuat",
            metadata={"chunk_id": "faq:a", "file_name": "faq.md", "heading": "QR"},
        )
        doc_a_duplicate = Document(
            page_content="  Noi   dung QR truy xuat ",
            metadata={"chunk_id": "faq:a-duplicate", "file_name": "faq.md", "heading": "QR"},
        )
        doc_b = Document(
            page_content="Noi dung VietGAP",
            metadata={"chunk_id": "vietgap:b", "file_name": "vietgap.md", "heading": "Nuoc"},
        )
        doc_far = Document(
            page_content="Qua xa",
            metadata={"chunk_id": "faq:far", "file_name": "faq.md", "heading": "Xa"},
        )
        candidates = [
            RetrievedContext(doc=doc_a, score=0.42, query="q1"),
            RetrievedContext(doc=doc_a_duplicate, score=0.2, query="q2"),
            RetrievedContext(doc=doc_b, score=0.7, query="q1"),
            RetrievedContext(doc=doc_far, score=1.8, query="q1"),
        ]

        selected = select_best_contexts(candidates, top_k=3, max_distance_threshold=1.0)

        self.assertEqual(len(selected), 2)
        self.assertEqual(selected[0].score, 0.2)
        self.assertEqual(selected[0].doc.metadata["heading"], "QR")
        self.assertEqual(selected[1].doc.metadata["heading"], "Nuoc")

    def test_normalize_content_hash_matches_whitespace_variants(self):
        self.assertEqual(
            normalize_content_hash("Noi   dung\nQR"),
            normalize_content_hash(" noi dung qr "),
        )

    def test_build_context_strips_metadata_from_prompt(self):
        doc = Document(
            page_content=(
                "# 4.2 Nguon nuoc\n"
                "source_path: C:\\data\\vietgap\\secret.md\n"
                "chunk_id: vietgap:secret:1\n"
                "Can kiem soat nguon nuoc tuoi."
            ),
            metadata={
                "source": "data/vietgap/secret.md",
                "file_name": "secret.md",
                "heading": "4.2 Nguon nuoc",
                "chunk_id": "vietgap:secret:1",
            },
        )
        service = RagService.__new__(RagService)

        context, used_contexts = service._build_context(
            [RetrievedContext(doc=doc, score=0.4641, query="q")],
            max_chunks=1,
        )

        self.assertIn("[TÀI LIỆU 1]", context)
        self.assertIn("Tiêu đề: 4.2 Nguon nuoc", context)
        self.assertIn("Can kiem soat nguon nuoc tuoi.", context)
        self.assertNotIn("secret.md", context)
        self.assertNotIn("distance", context.lower())
        self.assertNotIn("score", context.lower())
        self.assertNotIn("chunk_id", context.lower())
        self.assertNotIn("source_path", context.lower())
        self.assertNotIn("C:\\data", context)
        self.assertEqual(len(used_contexts), 1)
        self.assertIs(used_contexts[0].doc, doc)

    def test_build_context_only_uses_full_chunks_that_fit_prompt_budget(self):
        doc_a = Document(
            page_content="- Dong dau\n- Dong hai",
            metadata={"file_name": "a.md", "heading": "A", "chunk_id": "a:1"},
        )
        doc_b = Document(
            page_content="B" * 400,
            metadata={"file_name": "b.md", "heading": "B", "chunk_id": "b:1"},
        )
        service = RagService.__new__(RagService)
        original_limit = settings.MAX_CONTEXT_CHARS
        settings.MAX_CONTEXT_CHARS = 80
        try:
            context, used_contexts = service._build_context(
                [
                    RetrievedContext(doc=doc_a, score=0.1, query="q"),
                    RetrievedContext(doc=doc_b, score=0.2, query="q"),
                ],
                max_chunks=2,
            )
        finally:
            settings.MAX_CONTEXT_CHARS = original_limit

        self.assertIn("- Dong dau\n- Dong hai", context)
        self.assertNotIn("...", context)
        self.assertNotIn("BBBB", context)
        self.assertEqual([item.doc.metadata["chunk_id"] for item in used_contexts], ["a:1"])

    def test_sanitize_prompt_content_preserves_list_structure(self):
        raw = (
            "# Quy trinh\n"
            "source: data/vietgap/private.md\n"
            "- Buoc 1:  Ghi   nhat ky.\n"
            "- Buoc 2: Kiem tra nguon nuoc.\n"
            "chunk_id: vietgap:private:1\n"
            "C:/private/data/vietgap.md\n"
        )

        cleaned = sanitize_prompt_content(raw)

        self.assertIn("- Buoc 1: Ghi nhat ky.\n- Buoc 2: Kiem tra nguon nuoc.", cleaned)
        self.assertNotIn("source:", cleaned)
        self.assertNotIn("chunk_id", cleaned)
        self.assertNotIn("C:/private", cleaned)

    def test_build_sources_returns_sanitized_public_schema_and_prefers_richer_snippets(self):
        doc_a = Document(
            page_content=(
                "# QR\n"
                "source: data/faq/internal.md\n"
                "Huong dan tao ma QR cho lo san pham."
            ),
            metadata={
                "source": "data/faq/internal.md",
                "file_name": "faq.md",
                "heading": "QR",
                "chunk_id": "faq:1",
            },
        )
        doc_b = Document(
            page_content="## Nguon nuoc\nCan ghi nhan thong tin nguon nuoc tuoi.",
            metadata={"file_name": "vietgap.md", "heading": "Nguon nuoc", "chunk_id": "vietgap:1"},
        )
        doc_a_duplicate = Document(
            page_content="Noi dung trung",
            metadata={"file_name": "faq.md", "heading": "QR", "chunk_id": "faq:2"},
        )
        service = RagService.__new__(RagService)

        sources = service._build_sources(
            [
                RetrievedContext(doc=doc_a, score=0.1, query="q", adjusted_score=0.2),
                RetrievedContext(doc=doc_b, score=0.2, query="q", adjusted_score=0.2),
                RetrievedContext(doc=doc_a_duplicate, score=0.3, query="q"),
            ]
        )
        dumped = [source.model_dump() for source in sources]

        self.assertEqual(len(dumped), 2)
        self.assertEqual(list(dumped[0].keys()), ["file_name", "heading", "page", "snippet"])
        self.assertEqual(dumped[0]["file_name"], "vietgap.md")
        self.assertEqual(dumped[0]["heading"], "Nguon nuoc")
        self.assertEqual(dumped[1]["file_name"], "faq.md")
        self.assertNotIn("source", dumped[0])
        self.assertNotIn("chunk_id", dumped[0])
        self.assertNotIn("score", dumped[0])
        self.assertNotIn("data/faq", dumped[0]["snippet"])

    def test_sanitize_public_snippet_removes_metadata_and_caps_length(self):
        raw = (
            "# Heading\n"
            "score: 0.123\n"
            "source_path: C:/private/data/vietgap.md\n"
            "https://example.test/internal\n"
            + "A" * (MAX_PUBLIC_SNIPPET_CHARS + 50)
        )

        snippet = sanitize_public_snippet(raw)

        self.assertLessEqual(len(snippet), MAX_PUBLIC_SNIPPET_CHARS)
        self.assertNotIn("#", snippet)
        self.assertNotIn("score", snippet.lower())
        self.assertNotIn("source_path", snippet.lower())
        self.assertNotIn("http", snippet.lower())

    def test_chat_empty_strict_retrieval_returns_insufficient_without_sources(self):
        class EmptyService(RagService):
            def __init__(self):
                self.router = QuestionRouter()

            def _retrieve_contexts(self, question, top_k):
                return [], False

        service = EmptyService()

        result = service.chat("VietGAP yeu cau pH dat chinh xac bao nhieu?", top_k=2)

        self.assertEqual(result["answer"], INSUFFICIENT_DATA_MESSAGE)
        self.assertEqual(result["sources"], [])

    def test_chat_off_topic_returns_refusal_without_retrieval(self):
        class OffTopicService(RagService):
            def __init__(self):
                self.router = QuestionRouter()

            def _retrieve_contexts(self, question, top_k):
                raise AssertionError("off-topic questions must not retrieve")

        service = OffTopicService()

        result = service.chat("Bitcoin hom nay gia bao nhieu?", top_k=2)

        self.assertEqual(result["answer"], OFF_TOPIC_MESSAGE)
        self.assertEqual(result["sources"], [])

    def test_strict_rag_never_falls_back_to_general_llm_when_context_is_weak(self):
        class FakeOllama:
            general_called = False

            def generate_general_agriculture_answer(self, question):
                self.general_called = True
                return "general answer"

        class FakeService(RagService):
            def __init__(self):
                self.router = QuestionRouter()
                self.ollama_service = FakeOllama()

            def _retrieve_contexts(self, question, top_k):
                return [], False

        service = FakeService()

        result = service.chat("ACM co ho tro thanh toan blockchain khong?", top_k=2)

        self.assertEqual(result["answer"], INSUFFICIENT_DATA_MESSAGE)
        self.assertEqual(result["sources"], [])
        self.assertFalse(service.ollama_service.general_called)

    def test_rag_first_weak_context_falls_back_to_general_agriculture_llm(self):
        class FakeOllama:
            general_called = False

            def generate_general_agriculture_answer(self, question):
                self.general_called = True
                return "Đây là thông tin tham khảo chung; cần kiểm tra thực tế cây trồng trước khi xử lý."

        class FakeService(RagService):
            def __init__(self):
                self.router = QuestionRouter()
                self.ollama_service = FakeOllama()

            def _retrieve_contexts(self, question, top_k):
                return [], False

        service = FakeService()

        result = service.chat("Cà chua thường gặp sâu bệnh nào?", top_k=2)

        self.assertIn("tham khảo chung", result["answer"])
        self.assertEqual(result["sources"], [])
        self.assertTrue(service.ollama_service.general_called)

    def test_rag_first_insufficient_answer_falls_back_to_general_agriculture_llm(self):
        doc = Document(
            page_content=(
                "Ca chua thuong gap bo phan trang, sau duc qua va benh heo xanh. "
                "Nguoi trong can kiem tra la, than va qua de ghi nhan dau hieu sau benh."
            ),
            metadata={
                "category": "crop",
                "source": "data/crops/ca-chua/sau-benh-thuong-gap.md",
                "file_name": "sau-benh-thuong-gap.md",
                "heading": "Sau benh ca chua",
                "chunk_id": "crop:tomato",
            },
        )

        class FakeOllama:
            general_called = False
            generate_called = False

            def generate(self, prompt, chunks_count=0):
                self.generate_called = True
                return INSUFFICIENT_DATA_MESSAGE

            def generate_general_agriculture_answer(self, question):
                self.general_called = True
                return "Day la thong tin tham khao chung; ca chua co the gap nhieu nhom sau benh."

        class FakeService(RagService):
            def __init__(self):
                self.router = QuestionRouter()
                self.ollama_service = FakeOllama()

            def _retrieve_contexts(self, question, top_k):
                return [RetrievedContext(doc=doc, score=0.1, query=question)], False

        service = FakeService()

        result = service.chat("Ca chua thuong gap sau benh nao?", top_k=2)

        self.assertIn("tham khao chung", result["answer"])
        self.assertEqual(result["sources"], [])
        self.assertTrue(service.ollama_service.generate_called)
        self.assertTrue(service.ollama_service.general_called)

    def test_rag_first_empty_trailing_gap_or_too_short_answer_falls_back(self):
        doc = Document(
            page_content=(
                "Lua thuong gap ray nau, dao on va vang la sinh ly trong san xuat. "
                "Nguoi trong can quan sat ruong, la va than lua de ghi nhan dau hieu bat thuong."
            ),
            metadata={
                "category": "crop",
                "source": "data/crops/gao/sau-benh-thuong-gap.md",
                "file_name": "sau-benh-thuong-gap.md",
                "heading": "Sau benh lua",
                "chunk_id": "crop:rice",
            },
        )

        poor_answers = [
            "",
            "Sau.",
            "Ray nau.\n\nChua co du lieu ve: cach xu ly chi tiet.",
        ]

        for poor_answer in poor_answers:
            with self.subTest(poor_answer=poor_answer):
                class FakeOllama:
                    general_called = False
                    generate_called = False

                    def generate(self, prompt, chunks_count=0):
                        self.generate_called = True
                        return poor_answer

                    def generate_general_agriculture_answer(self, question):
                        self.general_called = True
                        return "Day la thong tin tham khao chung; can quan sat ruong truoc khi xu ly."

                class FakeService(RagService):
                    def __init__(self):
                        self.router = QuestionRouter()
                        self.ollama_service = FakeOllama()

                    def _retrieve_contexts(self, question, top_k):
                        return [RetrievedContext(doc=doc, score=0.1, query=question)], False

                service = FakeService()

                result = service.chat("Lua thuong gap sau benh nao?", top_k=2)

                self.assertIn("tham khao chung", result["answer"])
                self.assertEqual(result["sources"], [])
                self.assertTrue(service.ollama_service.generate_called)
                self.assertTrue(service.ollama_service.general_called)

    def test_strict_rag_insufficient_answer_never_falls_back_to_general_llm(self):
        doc = Document(
            page_content="ACM la he thong quan ly san xuat nong nghiep va truy xuat nguon goc.",
            metadata={
                "category": "acm",
                "source": "data/acm/tong-quan-he-thong.md",
                "file_name": "tong-quan-he-thong.md",
                "heading": "ACM",
                "chunk_id": "acm:overview",
            },
        )

        class FakeOllama:
            general_called = False

            def generate(self, prompt, chunks_count=0):
                return INSUFFICIENT_DATA_MESSAGE

            def generate_general_agriculture_answer(self, question):
                self.general_called = True
                return "general answer"

        class FakeService(RagService):
            def __init__(self):
                self.router = QuestionRouter()
                self.ollama_service = FakeOllama()

            def _retrieve_contexts(self, question, top_k):
                return [RetrievedContext(doc=doc, score=0.1, query=question)], False

        service = FakeService()

        result = service.chat("ACM co ho tro blockchain khong?", top_k=2)

        self.assertEqual(result["answer"], INSUFFICIENT_DATA_MESSAGE)
        self.assertEqual(result["sources"], [])
        self.assertFalse(service.ollama_service.general_called)

    def test_chat_model_insufficient_returns_without_sources(self):
        doc = Document(
            page_content="Noi dung co lien quan nhung model thay chua du.",
            metadata={"file_name": "vietgap.md", "heading": "Nguon nuoc", "chunk_id": "vietgap:1"},
        )

        class FakeOllama:
            def generate(self, prompt, chunks_count=0):
                return INSUFFICIENT_DATA_MESSAGE

        class FakeService(RagService):
            def __init__(self):
                self.ollama_service = FakeOllama()

            def _retrieve_contexts(self, question, top_k):
                return [RetrievedContext(doc=doc, score=0.1, query=question)], False

        service = FakeService()

        result = service.chat("VietGAP yeu cau nguon nuoc nhu the nao?", top_k=1)

        self.assertEqual(result["answer"], INSUFFICIENT_DATA_MESSAGE)
        self.assertEqual(result["sources"], [])

    def test_is_insufficient_answer_normalizes_variants(self):
        self.assertTrue(is_insufficient_answer(INSUFFICIENT_DATA_MESSAGE))
        self.assertEqual(
            CANONICAL_INSUFFICIENT_MESSAGE,
            "Tôi chưa có đủ dữ liệu trong tài liệu hiện tại.",
        )
        self.assertTrue(
            is_insufficient_answer("Tôi chưa có đủ dữ liệu trong tài liệu hiện tại để trả lời câu hỏi này.")
        )
        self.assertTrue(
            is_insufficient_answer("Tôi chưa có đủ dữ liệu trong tài liệu hiện tại.")
        )

    def test_system_prompt_contains_acm_chatbox_policy(self):
        self.assertIn("ACM AI Chatbox", SYSTEM_PROMPT)
        self.assertIn("chỉ được sử dụng NỘI DUNG HỖ TRỢ", SYSTEM_PROMPT)
        self.assertIn("Không bổ sung kiến thức bên ngoài", SYSTEM_PROMPT)
        self.assertIn("Không liệt kê nguồn trong nội dung trả lời", SYSTEM_PROMPT)
        self.assertIn("Không được tự tạo hoặc khẳng định", SYSTEM_PROMPT)
        self.assertIn(CANONICAL_INSUFFICIENT_MESSAGE, SYSTEM_PROMPT)

    def test_rag_prompt_template_keeps_required_sections(self):
        self.assertIn("/no_think", RAG_PROMPT_TEMPLATE)
        self.assertIn("=== NỘI DUNG HỖ TRỢ ===", RAG_PROMPT_TEMPLATE)
        self.assertIn("=== CÂU HỎI NGƯỜI DÙNG ===", RAG_PROMPT_TEMPLATE)
        self.assertIn("=== YÊU CẦU ĐẦU RA ===", RAG_PROMPT_TEMPLATE)
        self.assertIn("Không tự thêm phần nguồn", RAG_PROMPT_TEMPLATE)

    def test_general_agriculture_prompt_keeps_safety_boundaries(self):
        self.assertIn("kiến thức nông nghiệp phổ thông", GENERAL_AGRICULTURE_PROMPT)
        self.assertIn("không phải bằng tài liệu RAG nội bộ", GENERAL_AGRICULTURE_PROMPT)
        self.assertIn("Không tư vấn tên thuốc BVTV cụ thể", GENERAL_AGRICULTURE_PROMPT)
        self.assertIn("{question}", GENERAL_AGRICULTURE_PROMPT)

    def test_generate_general_agriculture_answer_uses_prompt_and_cleanup(self):
        service = OllamaService.__new__(OllamaService)
        captured = {}

        def fake_generate(prompt, chunks_count=0, num_predict=None):
            captured["prompt"] = prompt
            captured["chunks_count"] = chunks_count
            captured["num_predict"] = num_predict
            return "Theo tài liệu, Cà chua bị vàng lá do thiếu dinh dưỡng hoặc úng nước."

        service.generate = fake_generate

        answer = service.generate_general_agriculture_answer("Cà chua bị vàng lá do đâu?")

        self.assertIn("Cà chua bị vàng lá do đâu?", captured["prompt"])
        self.assertEqual(captured["chunks_count"], 0)
        self.assertEqual(captured["num_predict"], settings.GENERAL_AGRICULTURE_MAX_TOKENS)
        self.assertNotIn("Theo tài liệu", answer)
        self.assertIn("tham khảo chung", answer)
        self.assertIn("thiếu dinh dưỡng", answer)
        self.assertLessEqual(len(answer), 650)

    def test_general_agriculture_cleanup_removes_source_context_phrases(self):
        service = OllamaService.__new__(OllamaService)

        def fake_generate(prompt, chunks_count=0, num_predict=None):
            return "Dua tren ngu canh va source noi bo, cay can duoc theo doi them."

        service.generate = fake_generate

        answer = service.generate_general_agriculture_answer("Ca phe can dieu kien gi?")

        self.assertNotIn("ngu canh", answer.lower())
        self.assertNotIn("source", answer.lower())
        self.assertNotIn("context", answer.lower())

    def test_general_agriculture_cleanup_softens_vietgap_and_pesticide_specifics(self):
        service = OllamaService.__new__(OllamaService)

        def fake_generate(prompt, chunks_count=0, num_predict=None):
            return (
                "Cach nay chac chan dat VietGAP. "
                "Dung Confidor 100ml/ha va phun co dinh moi 7 ngay."
            )

        service.generate = fake_generate

        answer = service.generate_general_agriculture_answer("Ho tieu bi sau thi lam gi?")
        normalized = normalize_question(answer)

        self.assertNotIn("chac chan dat vietgap", normalized)
        self.assertNotIn("confidor", normalized)
        self.assertNotIn("100ml/ha", normalized)
        self.assertIn("tham khao chung", normalized)

    def test_general_agriculture_cleanup_removes_dangling_list_marker(self):
        answer = OllamaService._finalize_general_answer(
            "Đây là thông tin tham khảo chung:\n1. Tưới vừa đủ.\n2."
        )

        self.assertNotRegex(answer, r"\n?\s*\d+[\.)]\s*$")

    def test_high_confidence_route_uses_filtered_only_when_good_enough(self):
        class FakeStore:
            def __init__(self):
                self.filters = []

            def similarity_search_with_score(self, query, k, filter=None):
                self.filters.append(filter)
                if filter is not None:
                    filtered_doc_a = Document(
                        page_content="VietGAP yeu cau nguon nuoc sach va ghi chep day du.",
                        metadata={
                            "category": "vietgap",
                            "chunk_id": "vietgap:filtered-a",
                            "file_name": "vietgap.md",
                            "heading": "Filtered A",
                        },
                    )
                    filtered_doc_b = Document(
                        page_content="Nguon nuoc trong VietGAP can duoc kiem soat khi san xuat.",
                        metadata={
                            "category": "vietgap",
                            "chunk_id": "vietgap:filtered-b",
                            "file_name": "vietgap.md",
                            "heading": "Filtered B",
                        },
                    )
                    return [(filtered_doc_a, 0.3), (filtered_doc_b, 0.31)]
                all_doc = Document(
                    page_content="Noi dung all tot hon",
                    metadata={
                        "category": "faq",
                        "chunk_id": "faq:all",
                        "file_name": "faq.md",
                        "heading": "All",
                    },
                )
                duplicate_doc = Document(
                    page_content="Noi dung filtered",
                    metadata={
                        "chunk_id": "vietgap:filtered",
                        "file_name": "vietgap.md",
                        "heading": "Filtered duplicate",
                    },
                )
                return [(all_doc, 0.1), (duplicate_doc, 0.2)]

        service = RagService.__new__(RagService)
        service.chroma_store = FakeStore()

        contexts, is_definition = service._retrieve_contexts("VietGAP yeu cau nguon nuoc nhu the nao?", top_k=2)

        self.assertFalse(is_definition)
        self.assertEqual([item.doc.metadata["heading"] for item in contexts], ["Filtered A", "Filtered B"])
        self.assertIn({"category": "vietgap"}, service.chroma_store.filters)
        self.assertNotIn(None, service.chroma_store.filters)

    def test_high_confidence_route_still_searches_all_when_filtered_has_one_good_context(self):
        class FakeStore:
            def __init__(self):
                self.filters = []

            def similarity_search_with_score(self, query, k, filter=None):
                self.filters.append(filter)
                if filter is not None:
                    return []
                doc = Document(
                    page_content="Noi dung fallback",
                    metadata={
                        "chunk_id": "faq:fallback",
                        "file_name": "faq.md",
                        "heading": "Fallback",
                    },
                )
                return [(doc, 0.1)]

        service = RagService.__new__(RagService)
        service.chroma_store = FakeStore()

        contexts, is_definition = service._retrieve_contexts("VietGAP yeu cau nguon nuoc nhu the nao?", top_k=1)

        self.assertFalse(is_definition)
        self.assertEqual(len(contexts), 1)
        self.assertIn({"category": "vietgap"}, service.chroma_store.filters)
        self.assertIn(None, service.chroma_store.filters)

    def test_ollama_fallback_request_error_returns_insufficient_data(self):
        service = OllamaService.__new__(OllamaService)
        original_post = ollama_module.requests.post

        def raise_request_error(*args, **kwargs):
            raise requests.RequestException("connection failed")

        ollama_module.requests.post = raise_request_error
        try:
            with self.assertLogs("app.services.ollama_service", level="WARNING"):
                answer = service._fallback_generate("prompt")
        finally:
            ollama_module.requests.post = original_post

        self.assertEqual(answer, INSUFFICIENT_DATA_MESSAGE)


if __name__ == "__main__":
    unittest.main()
