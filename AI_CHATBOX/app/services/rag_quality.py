import re
from dataclasses import dataclass

from langchain_core.documents import Document

from app.services.source_sanitizer import sanitize_public_snippet


LOW_VALUE_CHUNK_MIN_CHARS = 80
LOW_VALUE_SNIPPET_MIN_CHARS = 40

_MARKDOWN_HEADING_RE = re.compile(r"^\s{0,3}#{1,6}\s+")
_NUMBERED_STEP_RE = re.compile(r"^\s*\d+[\.)]\s+", re.MULTILINE)
_BULLET_RE = re.compile(r"^\s*[-*+]\s+", re.MULTILINE)
_WHITESPACE_RE = re.compile(r"\s+")

_FAQ_SIGNALS = ("câu hỏi:", "trả lời:", "hoi:", "dap:", "faq")
_CHECKLIST_SIGNALS = ("checklist", "danh sách", "danh sach", "nội dung cần", "noi dung can")
_ACTION_SIGNALS = (
    "bước",
    "buoc",
    "cần",
    "can",
    "nên",
    "nen",
    "chọn",
    "chon",
    "nhập",
    "nhap",
    "lưu",
    "luu",
    "ghi",
    "kiểm tra",
    "kiem tra",
    "xác nhận",
    "xac nhan",
    "quét",
    "quet",
)


@dataclass(frozen=True)
class ChunkQuality:
    clean_text: str
    is_low_value: bool
    reasons: tuple[str, ...]
    information_score: int
    is_heading_only: bool
    has_signal: bool


def _normalize_text(text: str) -> str:
    return _WHITESPACE_RE.sub(" ", text).strip()


def _strip_heading_marker(text: str) -> str:
    return _MARKDOWN_HEADING_RE.sub("", text).strip()


def _has_signal(text: str, heading: str | None = None) -> tuple[bool, tuple[str, ...]]:
    combined = f"{heading or ''}\n{text}".lower()
    reasons: list[str] = []

    if any(signal in combined for signal in _FAQ_SIGNALS):
        reasons.append("faq_signal")
    if any(signal in combined for signal in _CHECKLIST_SIGNALS):
        reasons.append("checklist_signal")
    if any(signal in combined for signal in _ACTION_SIGNALS):
        reasons.append("action_signal")
    if _BULLET_RE.search(text):
        reasons.append("bullet_signal")
    if _NUMBERED_STEP_RE.search(text):
        reasons.append("numbered_step_signal")

    return bool(reasons), tuple(reasons)


def _is_heading_only(clean_text: str, heading: str | None) -> bool:
    if not clean_text:
        return False

    normalized_clean = _normalize_text(_strip_heading_marker(clean_text)).casefold()
    normalized_heading = _normalize_text(heading or "").casefold()
    if normalized_heading and normalized_clean == normalized_heading:
        return True

    lines = [_strip_heading_marker(line) for line in clean_text.splitlines() if line.strip()]
    return bool(lines) and all(
        line.casefold() == normalized_heading for line in lines if normalized_heading
    )


def snippet_information_score(text: str) -> int:
    clean = sanitize_public_snippet(text)
    if not clean:
        return 0

    score = min(len(clean), 240)
    score += clean.count(".") * 5
    score += clean.count(":") * 4
    score += len(_BULLET_RE.findall(text)) * 20
    score += len(_NUMBERED_STEP_RE.findall(text)) * 20
    return score


def analyze_text_quality(text: str, heading: str | None = None) -> ChunkQuality:
    clean_text = sanitize_public_snippet(text)
    normalized = _normalize_text(clean_text)
    reasons: list[str] = []

    if not normalized:
        reasons.append("empty")

    heading_only = _is_heading_only(normalized, heading)
    if heading_only:
        reasons.append("heading_only")

    has_signal, signal_reasons = _has_signal(text, heading)
    reasons.extend(signal_reasons)

    if len(normalized) < LOW_VALUE_CHUNK_MIN_CHARS:
        reasons.append("short")

    is_low_value = (
        not normalized
        or heading_only
        or (len(normalized) < LOW_VALUE_CHUNK_MIN_CHARS and not has_signal)
    )

    return ChunkQuality(
        clean_text=normalized,
        is_low_value=is_low_value,
        reasons=tuple(dict.fromkeys(reasons)),
        information_score=snippet_information_score(text),
        is_heading_only=heading_only,
        has_signal=has_signal,
    )


def analyze_document_quality(document: Document) -> ChunkQuality:
    return analyze_text_quality(
        document.page_content,
        heading=str(document.metadata.get("heading") or ""),
    )


def is_low_value_snippet(text: str, heading: str | None = None) -> bool:
    quality = analyze_text_quality(text, heading)
    if quality.is_heading_only or not quality.clean_text:
        return True
    if len(quality.clean_text) < LOW_VALUE_SNIPPET_MIN_CHARS and not quality.has_signal:
        return True
    return False
