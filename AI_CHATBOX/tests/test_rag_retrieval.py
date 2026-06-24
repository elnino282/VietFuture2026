import unittest

from langchain_core.documents import Document
import requests

import app.services.ollama_service as ollama_module
from app.config import settings
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

    def test_chat_empty_retrieval_returns_insufficient_without_sources(self):
        class EmptyService(RagService):
            def __init__(self):
                pass

            def _retrieve_contexts(self, question, top_k):
                return [], False

        service = EmptyService()

        result = service.chat("Cau hoi khong co tai lieu?", top_k=2)

        self.assertEqual(result["answer"], INSUFFICIENT_DATA_MESSAGE)
        self.assertEqual(result["sources"], [])

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
        self.assertTrue(
            is_insufficient_answer("Tôi chưa có đủ dữ liệu trong tài liệu hiện tại để trả lời câu hỏi này.")
        )

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
