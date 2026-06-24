import hashlib
import re
import unicodedata
from dataclasses import dataclass
from typing import Iterable, List, Optional

from langchain_core.documents import Document

from app.config import settings
from app.constants import INSUFFICIENT_DATA_MESSAGE
from app.services.rag_quality import analyze_document_quality

import logging

logger = logging.getLogger(__name__)

HIGH_CONFIDENCE_MIN_HITS = 2
SOFT_MAX_DISTANCE_THRESHOLD = 0.46
STRONG_BOOST_MIN_ABS = 0.08

# ---------------------------------------------------------------------------
# Definition-query detection
# ---------------------------------------------------------------------------

# Signals that indicate a user is asking for a definition or overview.
# Matched against the accent-stripped, lowercased query.
_DEFINITION_SIGNALS = [
    "la gi",
    "khai niem",
    "dinh nghia",
    "tong quan",
    "la the nao",
    "hieu nhu the nao",
]


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
    adjusted_score: Optional[float] = None
    reasons: tuple[str, ...] = ()


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
    "acm": [
        "acm",
        "farmtrace",        # legacy brand name -- still routes to acm
        "mua vu",
        "nong trai",
        "lien ket nong trai",
        "nhat ky san xuat",
        "nhat ky canh tac",
        "nhap kho",
        "ton kho",
        "dang ban",
        "san pham",
        "san thuong mai",
        "marketplace",
        "gio hang",
        "nguoi mua",
        "tao mua vu",
        "quan ly mua vu",
        "quan ly nong trai",
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
    "traceability": [
        "qr",
        "ma qr",
        "quet qr",
        "truy xuat",
        "truy xuat nguon goc",
        "nguon goc",
        "lo hang",
    ],
    "crop": [
        "ca chua",
        "gao",
        "lua",
        "rau an la",
        "sau benh",
        "benh hai",
        "thu hoach",
        "bao quan",
        "quy trinh san xuat",
    ],
}

_EXPANSIONS = [
    (["acm"], "quan ly nong trai mua vu nhat ky san xuat kho san pham truy xuat nguon goc"),
    (["tao mua vu"], "cac buoc tao mua vu quan ly mua vu tao mua vu moi nhap thong tin cay trong dien tich ngay bat dau luu mua vu"),
    (["nhat ky san xuat", "nhat ky"], "cac loai nhat ky dat nuoc giong cham soc bon phan thuoc bvtv sau benh thu hoach"),
    (["ca chua"], "ca chua nhom sau benh thuong gap bo phan trang sau duc qua heo xanh suong mai dom la than thu"),
    (["gio hang"], "lien ket gio hang nong trai mua san pham dat hang truy xuat tu gio hang"),
    (["thuoc sau", "phun thuoc", "thuoc bvtv"], "thuoc bao ve thuc vat"),
    (["ma qr", "qr code"], "truy xuat nguon goc"),
    (["ton kho", "kho hang"], "nhap kho"),
    (["cho nong san", "san thuong mai"], "marketplace dang ban san pham"),
    (["nhat ky canh tac"], "nhat ky san xuat"),
    (["nguon nuoc tuoi"], "nguon nuoc"),
]

# ---------------------------------------------------------------------------
# Rerank constants for definition queries
# ---------------------------------------------------------------------------

# Adjustment applied to the raw Chroma distance score.
# Chroma distance: lower = more relevant.  We subtract to boost, add to penalise.
_TONG_QUAN_FILE_BOOST   = -0.10   # strong boost: file_name contains "tong-quan"
_HEADING_BOOST          = -0.05   # boost: heading signals definition/overview
_PHAN_BIET_PENALTY      = +0.08   # penalty: file_name contains "phan-biet"
_TROUBLESHOOT_PENALTY   = +0.06   # penalty: loi-thuong-gap / checklist files
_FAQ_PENALTY            = +0.05   # penalty: faq category or faq/ in source path

# Heading substrings (accent-stripped, lowercase) that signal a definition chunk
_DEFINITION_HEADINGS = ["la gi", "tong quan", "khai niem", "muc dich", "gioi thieu"]


def normalize_text(text: str) -> str:
    """Lowercase, strip accents, normalise whitespace.  Used for all matching."""
    decomposed = unicodedata.normalize("NFD", text.lower())
    without_marks = "".join(char for char in decomposed if unicodedata.category(char) != "Mn")
    without_marks = without_marks.replace("đ", "d")
    return re.sub(r"\s+", " ", without_marks).strip()


def normalize_content_hash(text: str) -> str:
    normalized = normalize_text(text)
    return hashlib.sha1(normalized.encode("utf-8")).hexdigest()


def detect_definition_query(question: str) -> bool:
    """Return True when the question is asking for a definition or overview."""
    normalized = normalize_text(question)
    return any(signal in normalized for signal in _DEFINITION_SIGNALS)


def detect_intent(query: str) -> IntentResult:
    normalized = normalize_text(query)
    if "vietgap" in normalized:
        return IntentResult(category="vietgap", confidence="high", scores={"vietgap": HIGH_CONFIDENCE_MIN_HITS})
    if "acm" in normalized:
        return IntentResult(category="acm", confidence="high", scores={"acm": HIGH_CONFIDENCE_MIN_HITS})
    if any(term in normalized for term in ("qr", "ma qr", "truy xuat", "nguon goc")):
        return IntentResult(category="traceability", confidence="high", scores={"traceability": HIGH_CONFIDENCE_MIN_HITS})
    if any(term in normalized for term in ("ca chua", "gao", "lua", "rau an la")):
        return IntentResult(category="crop", confidence="high", scores={"crop": HIGH_CONFIDENCE_MIN_HITS})

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
            queries.append(expansion)
        if len(queries) >= 4:
            break
    return queries


def rerank_definition_candidates(
    candidates: List[RetrievedContext],
) -> List[RetrievedContext]:
    """Apply definition-aware score adjustments and re-sort.

    Only call this when ``detect_definition_query`` returned True.

    Strategy (Chroma distance — lower is better):
    - Subtract from score to BOOST a candidate.
    - Add to score to PENALISE a candidate.

    Penalty for FAQ / phan-biet files is suppressed when no ``tong-quan``
    candidate is present, so we always have something useful to return.
    """
    if not candidates:
        return candidates

    has_tong_quan = any(
        "tong-quan" in normalize_text(c.doc.metadata.get("file_name", ""))
        for c in candidates
    )

    adjusted: list[tuple[float, RetrievedContext]] = []
    for ctx in candidates:
        meta        = ctx.doc.metadata
        file_name   = normalize_text(meta.get("file_name", ""))
        heading     = normalize_text(meta.get("heading", ""))
        category    = str(meta.get("category", "")).lower()
        source      = normalize_text(meta.get("source", ""))
        base_score  = ctx.score if ctx.score is not None else 1.0

        adjustment = 0.0

        # --- Boost ---
        if "tong-quan" in file_name:
            adjustment += _TONG_QUAN_FILE_BOOST
        if any(h in heading for h in _DEFINITION_HEADINGS):
            adjustment += _HEADING_BOOST

        # --- Penalty (only when a tong-quan candidate exists) ---
        if has_tong_quan:
            if "phan-biet" in file_name:
                adjustment += _PHAN_BIET_PENALTY
            if "loi-thuong-gap" in file_name or "checklist" in file_name:
                adjustment += _TROUBLESHOOT_PENALTY
            if category == "faq" or "faq" in source.split("/"):
                adjustment += _FAQ_PENALTY

        final_score = base_score + adjustment

        logger.debug(
            "[RERANK-DEF] file=%s heading=%r original=%.4f adj=%+.4f final=%.4f",
            meta.get("file_name"),
            meta.get("heading"),
            base_score,
            adjustment,
            final_score,
        )
        adjusted.append((final_score, ctx))

    adjusted.sort(key=lambda t: t[0])
    return [ctx for _, ctx in adjusted]


def _normalized_source(doc: Document) -> str:
    source = str(doc.metadata.get("source") or doc.metadata.get("file_name") or "")
    return normalize_text(source).replace("\\", "/")


def _candidate_category(doc: Document) -> str:
    return str(doc.metadata.get("category") or "").strip().lower()


def _add_adjustment(reasons: list[str], adjustment: float, reason: str, delta: float) -> float:
    reasons.append(reason)
    return adjustment + delta


def _source_boost_for_question(question: str, doc: Document, reasons: list[str]) -> float:
    normalized_question = normalize_text(question)
    source = _normalized_source(doc)
    heading = normalize_text(str(doc.metadata.get("heading") or ""))
    category = _candidate_category(doc)
    adjustment = 0.0

    workflow_boosts = [
        (("tao mua vu", "mua vu"), "tao-mua-vu", "boost_source_tao_mua_vu", -0.10),
        (("nhat ky", "ghi chep"), "ghi-nhat-ky", "boost_source_ghi_nhat_ky", -0.10),
        (("thu hoach", "nhap kho", "kho"), "thu-hoach-va-nhap-kho", "boost_source_thu_hoach_nhap_kho", -0.10),
        (("gio hang", "nguoi mua", "dat hang"), "lien-ket-gio-hang-nong-trai", "boost_source_lien_ket_gio_hang", -0.12),
    ]
    for question_terms, source_term, reason, delta in workflow_boosts:
        if any(term in normalized_question for term in question_terms) and source_term in source:
            adjustment = _add_adjustment(reasons, adjustment, reason, delta)

    if "tao mua vu" in normalized_question and "cac buoc tao mua vu" in heading:
        adjustment = _add_adjustment(reasons, adjustment, "boost_heading_cac_buoc_tao_mua_vu", -0.10)
    if "sau khi tao mua vu" in normalized_question and "sau khi tao mua vu" in heading:
        adjustment = _add_adjustment(reasons, adjustment, "boost_heading_sau_khi_tao_mua_vu", -0.10)
    if "nhat ky" in normalized_question and (
        "cac loai nhat ky" in heading or "nhat ky nao quan trong" in heading
    ):
        adjustment = _add_adjustment(reasons, adjustment, "boost_heading_cac_loai_nhat_ky", -0.10)
    if "sau benh" in normalized_question and "nhom sau benh" in heading:
        adjustment = _add_adjustment(reasons, adjustment, "boost_heading_nhom_sau_benh", -0.10)
    if any(term in normalized_question for term in ("tao mua vu", "nhat ky", "sau benh")) and (
        "loi thuong gap" in heading or "rui ro thuong gap" in heading or heading == "muc dich"
    ):
        adjustment = _add_adjustment(reasons, adjustment, "penalty_heading_secondary", +0.06)

    if (
        any(term in normalized_question for term in ("qr", "ma qr", "truy xuat", "nguon goc"))
        and (category == "traceability" or "traceability/" in source)
    ):
        adjustment = _add_adjustment(reasons, adjustment, "boost_traceability_qr", -0.12)

    crop_rules = [
        (("ca chua",), "data/crops/ca-chua/", "boost_crop_ca_chua", "penalty_crop_mismatch_ca_chua"),
        (("gao", "lua"), "data/crops/gao/", "boost_crop_gao", "penalty_crop_mismatch_gao"),
        (("rau an la",), "data/crops/rau-an-la/", "boost_crop_rau_an_la", "penalty_crop_mismatch_rau_an_la"),
    ]
    for question_terms, source_term, boost_reason, penalty_reason in crop_rules:
        if any(term in normalized_question for term in question_terms):
            if source_term in source:
                adjustment = _add_adjustment(reasons, adjustment, boost_reason, -0.12)
            elif "data/crops/" in source:
                adjustment = _add_adjustment(reasons, adjustment, penalty_reason, +0.10)

    return adjustment


def adjust_candidate_scores(
    candidates: Iterable[RetrievedContext],
    question: str,
    intent_category: Optional[str],
    is_definition: bool,
) -> List[RetrievedContext]:
    adjusted: list[RetrievedContext] = []

    for candidate in candidates:
        doc = candidate.doc
        raw_score = candidate.score if candidate.score is not None else 1.0
        adjustment = 0.0
        reasons: list[str] = list(candidate.reasons)
        category = _candidate_category(doc)

        if intent_category:
            if category == intent_category:
                reasons.append("same_category")
                if candidate.score is not None and candidate.score <= SOFT_MAX_DISTANCE_THRESHOLD:
                    reasons.append("same_category_soft_fallback")
            elif category:
                adjustment = _add_adjustment(reasons, adjustment, "penalty_category_mismatch", +0.08)

        quality = analyze_document_quality(doc)
        if quality.is_low_value:
            adjustment = _add_adjustment(reasons, adjustment, "penalty_low_value", +0.10)
        if quality.is_heading_only:
            adjustment = _add_adjustment(reasons, adjustment, "penalty_heading_only", +0.15)
        if len(quality.clean_text) < 80:
            adjustment = _add_adjustment(reasons, adjustment, "penalty_short_chunk", +0.04)

        before_source_boost = adjustment
        adjustment += _source_boost_for_question(question, doc, reasons)
        source_boost_delta = adjustment - before_source_boost

        if is_definition:
            meta = doc.metadata
            file_name = normalize_text(meta.get("file_name", ""))
            heading = normalize_text(meta.get("heading", ""))
            source = _normalized_source(doc)
            if "tong-quan" in file_name:
                adjustment = _add_adjustment(reasons, adjustment, "boost_definition_tong_quan", _TONG_QUAN_FILE_BOOST)
            if any(h in heading for h in _DEFINITION_HEADINGS):
                adjustment = _add_adjustment(reasons, adjustment, "boost_definition_heading", _HEADING_BOOST)
            if "phan-biet" in file_name:
                adjustment = _add_adjustment(reasons, adjustment, "penalty_definition_phan_biet", _PHAN_BIET_PENALTY)
            if "loi-thuong-gap" in file_name or "checklist" in file_name:
                adjustment = _add_adjustment(reasons, adjustment, "penalty_definition_troubleshoot", _TROUBLESHOOT_PENALTY)
            if category == "faq" or "faq" in source.split("/"):
                adjustment = _add_adjustment(reasons, adjustment, "penalty_definition_faq", _FAQ_PENALTY)

        has_strong_boost = source_boost_delta <= -STRONG_BOOST_MIN_ABS or any(
            reason.startswith("boost_definition") for reason in reasons
        )
        if has_strong_boost and candidate.score is not None and candidate.score <= SOFT_MAX_DISTANCE_THRESHOLD:
            reasons.append("strong_boost_soft_fallback")

        final_score = max(raw_score + adjustment, 0.0)
        unique_reasons = tuple(dict.fromkeys(reasons))

        if settings.DEBUG_RAG:
            logger.info(
                "[DEBUG_RAG] raw_distance=%s adjusted_distance=%.4f adjustment=%+.4f reasons=%s "
                "category=%s source=%s heading=%s",
                f"{candidate.score:.4f}" if candidate.score is not None else None,
                final_score,
                adjustment,
                ",".join(unique_reasons) or "none",
                category,
                doc.metadata.get("source") or doc.metadata.get("file_name"),
                doc.metadata.get("heading"),
            )

        adjusted.append(
            RetrievedContext(
                doc=doc,
                score=candidate.score,
                query=candidate.query,
                adjusted_score=final_score,
                reasons=unique_reasons,
            )
        )

    return adjusted


def _effective_score(candidate: RetrievedContext) -> float:
    score = candidate.adjusted_score if candidate.adjusted_score is not None else candidate.score
    return float("inf") if score is None else score


def _within_threshold(candidate: RetrievedContext, max_distance_threshold: Optional[float]) -> bool:
    if max_distance_threshold is None or candidate.score is None:
        return True
    if candidate.score <= max_distance_threshold:
        return True
    if candidate.score <= SOFT_MAX_DISTANCE_THRESHOLD and (
        "same_category_soft_fallback" in candidate.reasons
        or "strong_boost_soft_fallback" in candidate.reasons
    ):
        return True
    return False


def select_best_contexts(
    candidates: Iterable[RetrievedContext],
    top_k: int,
    max_distance_threshold: Optional[float],
) -> List[RetrievedContext]:
    filtered: list[RetrievedContext] = []
    for candidate in candidates:
        if not _within_threshold(candidate, max_distance_threshold):
            continue
        filtered.append(candidate)

    sorted_candidates = sorted(
        filtered,
        key=_effective_score,
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
