import re

from app.constants import MAX_PROMPT_CHUNK_CHARS, MAX_PUBLIC_SNIPPET_CHARS


METADATA_PATTERNS = [
    re.compile(r"\b(?:score|distance|chunk_id|source_path|source|embedding|metadata)\b\s*[:=].*", re.IGNORECASE),
    re.compile(r"\b(?:document_id|doc_id|vector_id|id)\b\s*[:=]\s*[\w:./\\-]+", re.IGNORECASE),
    re.compile(r"https?://\S+", re.IGNORECASE),
    re.compile(r"file://\S+", re.IGNORECASE),
    re.compile(r"\b[A-Za-z]:[\\/][^\s]+"),
    re.compile(r"(?:^|\s)(?:\.{0,2}[\\/])?(?:data|app|tests|scripts|ui demo)[\\/][^\s]+", re.IGNORECASE),
]

_MARKDOWN_HEADING_PATTERN = re.compile(r"^\s{0,3}#{1,6}\s+")


def normalize_whitespace(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def _contains_metadata_pattern(text: str) -> bool:
    return any(pattern.search(text) for pattern in METADATA_PATTERNS)


def remove_metadata_lines(text: str) -> str:
    clean_lines: list[str] = []
    for raw_line in text.replace("\r\n", "\n").split("\n"):
        line = raw_line.strip()
        if not line:
            continue
        if _contains_metadata_pattern(line):
            continue
        clean_lines.append(_MARKDOWN_HEADING_PATTERN.sub("", line).strip())
    return "\n".join(line for line in clean_lines if line)


def _strip_inline_metadata(text: str) -> str:
    cleaned = text
    for pattern in METADATA_PATTERNS:
        cleaned = pattern.sub("", cleaned)
    return cleaned


def sanitize_prompt_content(text: str, max_chars: int = MAX_PROMPT_CHUNK_CHARS) -> str:
    cleaned = remove_metadata_lines(text)
    cleaned = _strip_inline_metadata(cleaned)
    cleaned = normalize_whitespace(cleaned)
    return cleaned[:max_chars].rstrip()


def sanitize_public_snippet(text: str, max_chars: int = MAX_PUBLIC_SNIPPET_CHARS) -> str:
    cleaned = remove_metadata_lines(text)
    cleaned = _strip_inline_metadata(cleaned)
    cleaned = normalize_whitespace(cleaned)
    return cleaned[:max_chars].rstrip()
