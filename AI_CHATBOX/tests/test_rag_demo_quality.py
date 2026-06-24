import unittest

from langchain_core.documents import Document

from app.constants import INSUFFICIENT_DATA_MESSAGE
from app.services.rag_quality import (
    analyze_document_quality,
    is_low_value_snippet,
    snippet_information_score,
)
from app.services.rag_retrieval import (
    RetrievedContext,
    adjust_candidate_scores,
    select_best_contexts,
)
from app.services.rag_service import RagService


class RagDemoQualityTests(unittest.TestCase):
    def test_low_value_helper_detects_heading_only_and_preserves_short_faq(self):
        heading_only = Document(
            page_content="## Mục đích",
            metadata={"heading": "Mục đích", "file_name": "sample.md"},
        )
        faq = Document(
            page_content="## Câu hỏi: ACM là gì?\n\nTrả lời: ACM hỗ trợ quản lý mùa vụ.",
            metadata={"heading": "Câu hỏi: ACM là gì?", "file_name": "faq-acm.md"},
        )

        heading_quality = analyze_document_quality(heading_only)
        faq_quality = analyze_document_quality(faq)

        self.assertTrue(heading_quality.is_low_value)
        self.assertIn("heading_only", heading_quality.reasons)
        self.assertFalse(faq_quality.is_low_value)
        self.assertIn("faq_signal", faq_quality.reasons)
        self.assertFalse(is_low_value_snippet(faq.page_content, faq.metadata["heading"]))

    def test_snippet_information_score_prefers_content_rich_snippets(self):
        weak = "## Mục đích"
        rich = "- Tên sản phẩm.\n- Tên nông trại sản xuất.\n- Ngày thu hoạch."

        self.assertLess(snippet_information_score(weak), snippet_information_score(rich))

    def test_adjusted_scoring_boosts_traceability_for_qr_question(self):
        trace_doc = Document(
            page_content="Trang QR hiển thị tên sản phẩm, nông trại và ngày thu hoạch.",
            metadata={
                "category": "traceability",
                "source": "data/traceability/thong-tin-hien-thi-khi-quet-qr.md",
                "file_name": "thong-tin-hien-thi-khi-quet-qr.md",
                "heading": "Người mua quét QR thấy gì?",
                "chunk_id": "traceability:qr",
            },
        )
        faq_doc = Document(
            page_content="QR là mã để mở trang web.",
            metadata={
                "category": "faq",
                "source": "data/faq/faq-truy-xuat-nguon-goc.md",
                "file_name": "faq-truy-xuat-nguon-goc.md",
                "heading": "QR",
                "chunk_id": "faq:qr",
            },
        )
        adjusted = adjust_candidate_scores(
            [
                RetrievedContext(trace_doc, 0.45, "Mã QR truy xuất nguồn gốc hiển thị gì?"),
                RetrievedContext(faq_doc, 0.39, "Mã QR truy xuất nguồn gốc hiển thị gì?"),
            ],
            question="Mã QR truy xuất nguồn gốc hiển thị gì?",
            intent_category="traceability",
            is_definition=False,
        )

        selected = select_best_contexts(adjusted, top_k=1, max_distance_threshold=0.42)

        self.assertEqual(selected[0].doc.metadata["category"], "traceability")
        self.assertLess(selected[0].adjusted_score, selected[0].score)
        self.assertTrue(any("traceability" in reason for reason in selected[0].reasons))

    def test_soft_threshold_allows_same_category_or_strong_boost_only(self):
        same_category = Document(
            page_content="Người mua thêm sản phẩm vào giỏ hàng rồi xác nhận đặt hàng.",
            metadata={
                "category": "acm",
                "source": "data/acm/lien-ket-gio-hang-nong-trai.md",
                "file_name": "lien-ket-gio-hang-nong-trai.md",
                "heading": "Mua sản phẩm từ nông trại",
                "chunk_id": "acm:cart",
            },
        )
        mismatch = Document(
            page_content="Thông tin chung không liên quan.",
            metadata={
                "category": "faq",
                "source": "data/faq/faq-admin.md",
                "file_name": "faq-admin.md",
                "heading": "Chung",
                "chunk_id": "faq:admin",
            },
        )
        adjusted = adjust_candidate_scores(
            [
                RetrievedContext(same_category, 0.455, "Người mua liên kết giỏ hàng với nông trại thế nào?"),
                RetrievedContext(mismatch, 0.455, "Người mua liên kết giỏ hàng với nông trại thế nào?"),
            ],
            question="Người mua liên kết giỏ hàng với nông trại thế nào?",
            intent_category="acm",
            is_definition=False,
        )

        selected = select_best_contexts(adjusted, top_k=2, max_distance_threshold=0.42)

        self.assertEqual([item.doc.metadata["chunk_id"] for item in selected], ["acm:cart"])
        self.assertIn("same_category_soft_fallback", selected[0].reasons)

    def test_crop_question_prefers_matching_crop_path(self):
        tomato = Document(
            page_content="Bọ phấn trắng, sâu đục quả và bệnh héo xanh thường gặp trên cà chua.",
            metadata={
                "category": "crop",
                "source": "data/crops/ca-chua/sau-benh-thuong-gap.md",
                "file_name": "sau-benh-thuong-gap.md",
                "heading": "Nhóm sâu bệnh thường gặp",
                "chunk_id": "crop:tomato",
            },
        )
        rice = Document(
            page_content="Rầy nâu thường gặp trên lúa.",
            metadata={
                "category": "crop",
                "source": "data/crops/gao/sau-benh-thuong-gap.md",
                "file_name": "sau-benh-thuong-gap.md",
                "heading": "Nhóm sâu bệnh thường gặp",
                "chunk_id": "crop:rice",
            },
        )
        adjusted = adjust_candidate_scores(
            [
                RetrievedContext(tomato, 0.44, "Cà chua thường gặp sâu bệnh nào?"),
                RetrievedContext(rice, 0.39, "Cà chua thường gặp sâu bệnh nào?"),
            ],
            question="Cà chua thường gặp sâu bệnh nào?",
            intent_category="crop",
            is_definition=False,
        )

        selected = select_best_contexts(adjusted, top_k=1, max_distance_threshold=0.42)

        self.assertEqual(selected[0].doc.metadata["source"], "data/crops/ca-chua/sau-benh-thuong-gap.md")
        self.assertIn("boost_crop_ca_chua", selected[0].reasons)

    def test_postprocess_removes_forbidden_prefixes_question_echo_and_trailing_gap(self):
        raw = (
            "VietGAP là gì?\n"
            "Theo tài liệu, Viện Good Agricultural Practices là thực hành nông nghiệp tốt tại Việt Nam. "
            "Dựa trên ngữ cảnh, VietGAP hỗ trợ sản xuất an toàn. "
            "Chưa có dữ liệu về: hồ sơ chứng nhận."
        )

        answer = RagService._postprocess_answer(raw, question="VietGAP là gì?", is_definition=True)

        self.assertNotIn("VietGAP là gì?", answer)
        self.assertNotIn("Theo tài liệu", answer)
        self.assertNotIn("ngữ cảnh", answer)
        self.assertNotIn("Chưa có dữ liệu về", answer)
        self.assertNotIn("Viện Good Agricultural Practices", answer)
        self.assertIn("Vietnamese Good Agricultural Practices", answer)

    def test_postprocess_canonicalizes_demo_journal_contents(self):
        answer = RagService._postprocess_answer(
            "Ghi chép hoạt động trong mùa vụ.",
            question="Nhật ký sản xuất cần ghi những nội dung gì?",
            is_definition=False,
        )

        self.assertIn("đất, nước, giống", answer)
        self.assertIn("phân bón", answer)
        self.assertIn("thuốc BVTV", answer)
        self.assertIn("thu hoạch", answer)

    def test_postprocess_canonicalizes_cart_farm_link_answer(self):
        answer = RagService._postprocess_answer(
            "Người mua chọn sản phẩm và xem nguồn gốc.",
            question="Người mua liên kết giỏ hàng với nông trại như thế nào?",
            is_definition=False,
        )

        self.assertIn("giỏ hàng", answer)
        self.assertIn("nông trại", answer)
        self.assertIn("đặt hàng", answer)

    def test_build_sources_skips_low_value_snippets_and_prefers_richer_sources(self):
        heading_only = Document(
            page_content="## Mục đích",
            metadata={"file_name": "weak.md", "heading": "Mục đích", "chunk_id": "weak"},
        )
        rich = Document(
            page_content="- Tên sản phẩm.\n- Tên nông trại sản xuất.\n- Ngày thu hoạch.",
            metadata={"file_name": "qr.md", "heading": "Người mua quét QR thấy gì?", "chunk_id": "rich"},
        )
        service = RagService.__new__(RagService)

        sources = service._build_sources(
            [
                RetrievedContext(heading_only, 0.1, "q"),
                RetrievedContext(rich, 0.2, "q"),
            ]
        )

        self.assertEqual(len(sources), 1)
        self.assertEqual(sources[0].file_name, "qr.md")

    def test_blockchain_fallback_exact_match_remains_canonical(self):
        self.assertEqual(INSUFFICIENT_DATA_MESSAGE, "Tôi chưa có đủ dữ liệu trong tài liệu hiện tại.")


if __name__ == "__main__":
    unittest.main()
