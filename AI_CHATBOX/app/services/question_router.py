import re
import unicodedata
from dataclasses import dataclass
from typing import Literal

from app.config import settings
from app.services.rag_quality import analyze_document_quality


RouteMode = Literal["strict_rag", "rag_first", "general_agriculture_llm", "off_topic"]
RouteConfidence = Literal["high", "medium", "low"]


@dataclass(frozen=True)
class QuestionRoute:
    mode: RouteMode
    category: str | None
    confidence: RouteConfidence
    reason: str


def normalize_question(text: str) -> str:
    decomposed = unicodedata.normalize("NFD", text.casefold())
    without_marks = "".join(
        char for char in decomposed if unicodedata.category(char) != "Mn"
    )
    without_marks = without_marks.replace("đ", "d")
    return re.sub(r"\s+", " ", without_marks).strip()


STRICT_CATEGORY_TERMS: dict[str, tuple[str, ...]] = {
    "vietgap": (
        "vietgap",
        "chung nhan",
        "ho so chung nhan",
        "checklist",
        "tieu chuan",
        "quy dinh",
        "yeu cau",
        "phap ly",
        "tuân thu",
        "tuan thu",
    ),
    "acm": (
        "acm",
        "he thong",
        "mua vu",
        "nong trai",
        "trang trai",
        "nhat ky san xuat",
        "nhat ky canh tac",
        "nhap kho",
        "ton kho",
        "dang ban",
        "gio hang",
        "nguoi mua",
        "thanh toan",
        "blockchain",
        "tai khoan",
        "quan ly",
    ),
    "traceability": (
        "qr",
        "ma qr",
        "quet qr",
        "truy xuat",
        "truy xuat nguon goc",
        "nguon goc",
        "du lieu ca nhan",
        "thong tin ca nhan",
    ),
}

RAG_FIRST_CROP_TERMS = (
    "ca chua",
    "rau an la",
    "gao",
    "lua",
)

UNDOCUMENTED_CROP_TERMS = (
    "ca phe",
    "ho tieu",
    "sau rieng",
    "ngo",
    "bap",
    "thanh long",
    "chuoi",
)

GENERAL_CROP_TERMS = RAG_FIRST_CROP_TERMS + UNDOCUMENTED_CROP_TERMS

RAG_FIRST_CROP_QUESTION_TERMS = (
    "thuong gap",
    "thuong gap sau benh",
    "sau benh nao",
    "quy trinh san xuat",
    "lich giai doan",
    "thu hoach bao quan",
    "cham soc",
)

GENERAL_AGRICULTURE_TERMS = (
    "cay",
    "rau",
    "lua",
    "gao",
    "ca chua",
    "ca phe",
    "ho tieu",
    "sau rieng",
    "ngo",
    "bap",
    "thanh long",
    "chuoi",
    "dat",
    "nuoc",
    "tuoi",
    "phan bon",
    "thieu dam",
    "thieu kali",
    "dinh duong",
    "vang la",
    "xoan la",
    "sau xanh",
    "sau",
    "benh",
    "con trung",
    "nam",
    "than",
    "qua",
    "hat",
    "cay con",
    "trong",
    "canh tac",
    "cai tao dat",
    "chai cung",
    "bac mau",
    "nong nghiep",
    "khi hau",
    "mat do",
    "sinh truong",
)

GENERAL_SYMPTOM_TERMS = (
    "bi",
    "can",
    "do dau",
    "tai sao",
    "bieu hien",
    "la gi",
    "lam sao",
    "cach",
    "nen",
    "nhu the nao",
    "cham soc",
    "cai tao",
    "luu y",
    "dieu kien",
    "bao nhieu",
    "bao lau",
    "uoc tinh",
    "lieu luong",
    "thoi gian",
    "mat do",
    "bon phan",
    "tuoi nuoc",
)

OFFICIAL_REQUIREMENT_TERMS = (
    "chinh thuc",
    "bat buoc",
    "tieu chuan",
    "quy dinh",
    "phap luat",
    "phap ly",
    "chung nhan",
    "ho so",
    "kiem dinh",
    "danh gia",
    "dat chuan",
    "checklist",
)

OFF_TOPIC_TERMS = (
    "bitcoin",
    "gia vang",
    "chung khoan",
    "react",
    "python",
    "lap trinh",
    "bai tho",
    "tinh yeu",
    "bong da",
    "thoi tiet hom nay",
)


class QuestionRouter:
    def route(self, question: str) -> QuestionRoute:
        normalized = normalize_question(question)

        strict_category = self._strict_category(normalized)
        if strict_category:
            return QuestionRoute(
                mode="strict_rag",
                category=strict_category,
                confidence="high",
                reason=f"strict signal for {strict_category}",
            )

        if any(term in normalized for term in OFF_TOPIC_TERMS) and not self._is_agriculture(normalized):
            return QuestionRoute(
                mode="off_topic",
                category=None,
                confidence="high",
                reason="non-agriculture topic",
            )

        if self._is_documented_crop_question(normalized):
            return QuestionRoute(
                mode="rag_first",
                category="crop",
                confidence="medium",
                reason="crop document candidate",
            )

        if self._is_general_agriculture(normalized):
            return QuestionRoute(
                mode="general_agriculture_llm",
                category="general_agriculture",
                confidence="medium",
                reason="general agriculture question",
            )

        return QuestionRoute(
            mode="off_topic",
            category=None,
            confidence="low",
            reason="no agriculture or ACM signal",
        )

    @staticmethod
    def _is_agriculture(normalized: str) -> bool:
        return any(term in normalized for term in GENERAL_AGRICULTURE_TERMS)

    @staticmethod
    def _is_documented_crop_question(normalized: str) -> bool:
        return any(term in normalized for term in RAG_FIRST_CROP_TERMS) and (
            any(term in normalized for term in RAG_FIRST_CROP_QUESTION_TERMS)
            or any(term in normalized for term in GENERAL_SYMPTOM_TERMS)
        )

    def _is_general_agriculture(self, normalized: str) -> bool:
        return self._is_agriculture(normalized) and any(
            term in normalized for term in GENERAL_SYMPTOM_TERMS
        )

    @staticmethod
    def _strict_category(normalized: str) -> str | None:
        if "vietgap" in normalized:
            return "vietgap"
        if any(term in normalized for term in STRICT_CATEGORY_TERMS["traceability"]):
            return "traceability"
        if "acm" in normalized:
            return "acm"
        if any(term in normalized for term in STRICT_CATEGORY_TERMS["acm"]) and (
            "he thong" in normalized
            or "mua vu" in normalized
            or "nhat ky" in normalized
            or "nhap kho" in normalized
            or "dang ban" in normalized
            or "gio hang" in normalized
            or "blockchain" in normalized
        ):
            return "acm"
        if any(term in normalized for term in OFFICIAL_REQUIREMENT_TERMS) and (
            any(term in normalized for term in GENERAL_AGRICULTURE_TERMS)
            or any(term in normalized for term in GENERAL_CROP_TERMS)
        ):
            return "vietgap"
        return None


def _context_category(context) -> str:
    return str(context.doc.metadata.get("category") or "").strip().lower()


def _context_source(context) -> str:
    value = context.doc.metadata.get("source") or context.doc.metadata.get("file_name") or ""
    return normalize_question(str(value)).replace("\\", "/")


def _distance_is_acceptable(context) -> bool:
    if context.score is None or settings.MAX_DISTANCE_THRESHOLD is None:
        return True
    if context.score <= settings.MAX_DISTANCE_THRESHOLD:
        return True
    return context.adjusted_score is not None and context.adjusted_score <= settings.MAX_DISTANCE_THRESHOLD


def _crop_source_matches(question: str, context) -> bool:
    normalized = normalize_question(question)
    source = _context_source(context)
    crop_paths = [
        (("ca chua",), "data/crops/ca-chua/"),
        (("gao", "lua"), "data/crops/gao/"),
        (("rau an la",), "data/crops/rau-an-la/"),
    ]
    for question_terms, source_term in crop_paths:
        if any(term in normalized for term in question_terms):
            return source_term in source
    return True


def has_good_rag_context(
    contexts,
    route: QuestionRoute,
    question: str,
) -> bool:
    if not contexts:
        return False

    for context in contexts:
        quality = analyze_document_quality(context.doc)
        if quality.is_heading_only or not quality.clean_text:
            continue
        if quality.is_low_value and not quality.has_signal:
            continue
        if len(quality.clean_text) < 40 and not quality.has_signal:
            continue
        if not _distance_is_acceptable(context):
            continue
        category = _context_category(context)
        if route.mode == "strict_rag" and route.category and category and category != route.category:
            continue
        if route.category == "crop" and category == "crop" and not _crop_source_matches(question, context):
            continue
        return True

    return False
