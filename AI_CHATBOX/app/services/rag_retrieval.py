import hashlib
import re
import unicodedata
from dataclasses import dataclass
from typing import Iterable, List, Optional

from langchain_core.documents import Document

from app.constants import INSUFFICIENT_DATA_MESSAGE

HIGH_CONFIDENCE_MIN_HITS = 2


@dataclass(frozen=True)
class IntentResult:
    category: Optional[str]
    confidence: str
    scores: dict[str, int]


@dataclass(frozen=True)
class RetrievedContext:
    doc: Document
    score: Optional[float]
    query: str


_INTENT_KEYWORDS = {
    "vietgap": [
        "vietgap",
        "tieu chuan",
        "nguon nuoc",
        "dat",
        "phan bon",
        "thuoc bao ve thuc vat",
        "thuoc bvtv",
        "thu hoach",
        "ho so",
        "nhat ky",
        "kiem tra noi bo",
    ],
    "farmtrace": [
        "farmtrace",
        "mua vu",
        "admin",
        "qr",
        "truy xuat",
        "nhap kho",
        "ton kho",
        "dang ban",
        "san thuong mai",
        "marketplace",
        "nguoi mua",
    ],
    "faq": [
        "faq",
        "cau hoi",
        "hoi dap",
        "la gi",
        "vi sao",
        "khi nao",
        "nhu the nao",
    ],
}

_EXPANSIONS = [
    (["thuoc sau", "phun thuoc", "thuoc bvtv"], "thuoc bao ve thuc vat"),
    (["ma qr", "qr code"], "truy xuat nguon goc"),
    (["ton kho", "kho hang"], "nhap kho"),
    (["cho nong san", "san thuong mai"], "marketplace dang ban san pham"),
    (["nhat ky canh tac"], "nhat ky san xuat"),
    (["nguon nuoc tuoi"], "nguon nuoc"),
]


def normalize_text(text: str) -> str:
    decomposed = unicodedata.normalize("NFD", text.lower())
    without_marks = "".join(char for char in decomposed if unicodedata.category(char) != "Mn")
    without_marks = without_marks.replace("đ", "d")
    return re.sub(r"\s+", " ", without_marks).strip()


def normalize_content_hash(text: str) -> str:
    normalized = normalize_text(text)
    return hashlib.sha1(normalized.encode("utf-8")).hexdigest()


def detect_intent(query: str) -> IntentResult:
    normalized = normalize_text(query)
    scores: dict[str, int] = {}
    for category, keywords in _INTENT_KEYWORDS.items():
        scores[category] = sum(1 for keyword in keywords if keyword in normalized)

    best_category = max(scores, key=scores.get)
    best_score = scores[best_category]
    if best_score >= HIGH_CONFIDENCE_MIN_HITS:
        return IntentResult(category=best_category, confidence="high", scores=scores)
    return IntentResult(category=None, confidence="low", scores=scores)


def expand_query(query: str) -> List[str]:
    normalized = normalize_text(query)
    queries = [query]
    for triggers, expansion in _EXPANSIONS:
        if any(trigger in normalized for trigger in triggers) and expansion not in normalized:
            queries.append(f"{query} {expansion}")
        if len(queries) >= 4:
            break
    return queries


def select_best_contexts(
    candidates: Iterable[RetrievedContext],
    top_k: int,
    max_distance_threshold: Optional[float],
) -> List[RetrievedContext]:
    filtered: list[RetrievedContext] = []
    for candidate in candidates:
        if (
            max_distance_threshold is not None
            and candidate.score is not None
            and candidate.score > max_distance_threshold
        ):
            continue
        filtered.append(candidate)

    sorted_candidates = sorted(
        filtered,
        key=lambda item: float("inf") if item.score is None else item.score,
    )

    selected: list[RetrievedContext] = []
    seen_chunk_ids: set[str] = set()
    seen_hashes: set[str] = set()
    for candidate in sorted_candidates:
        chunk_id = candidate.doc.metadata.get("chunk_id")
        content_hash = normalize_content_hash(candidate.doc.page_content)
        if chunk_id and chunk_id in seen_chunk_ids:
            continue
        if content_hash in seen_hashes:
            continue
        selected.append(candidate)
        if chunk_id:
            seen_chunk_ids.add(chunk_id)
        seen_hashes.add(content_hash)
        if len(selected) >= top_k:
            break
    return selected


def is_insufficient_answer(answer: str) -> bool:
    normalized_answer = normalize_text(answer.strip())
    normalized_fallback = normalize_text(INSUFFICIENT_DATA_MESSAGE)
    return normalized_answer == normalized_fallback or "chua co du du lieu" in normalized_answer
