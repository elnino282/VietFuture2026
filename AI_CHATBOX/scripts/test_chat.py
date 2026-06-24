import sys
from dataclasses import dataclass
from pathlib import Path

import requests

ROOT_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT_DIR))

from app.config import settings  # noqa: E402
from app.constants import INSUFFICIENT_DATA_MESSAGE  # noqa: E402
from app.services.rag_service import RagService  # noqa: E402


FORBIDDEN_PHRASES = [
    "Viện Good Agricultural Practices",
    "Theo tài liệu",
    "Dựa trên ngữ cảnh",
    "Nội dung hỗ trợ",
    "context",
]

CATEGORY_FILE_HINTS = {
    "vietgap": {"tong-quan-vietgap.md"},
    "faq": {"faq-acm.md"},
    "acm": {
        "tong-quan-he-thong.md",
        "tao-mua-vu.md",
        "ghi-nhat-ky-san-xuat.md",
        "thu-hoach-va-nhap-kho.md",
        "lien-ket-gio-hang-nong-trai.md",
    },
    "traceability": {"thong-tin-hien-thi-khi-quet-qr.md"},
    "crop": {"sau-benh-thuong-gap.md"},
}


@dataclass(frozen=True)
class DemoCase:
    question: str
    required_keywords: tuple[str, ...] = ()
    expected_source_category: str | None = None
    expected_source_file: str | None = None
    forbidden_source_files: tuple[str, ...] = ()
    max_answer_chars: int = 650
    exact_fallback: bool = False


DEMO_CASES = [
    DemoCase(
        question="VietGAP là gì?",
        required_keywords=("Vietnamese Good Agricultural Practices", "Thực hành nông nghiệp tốt"),
        expected_source_category="vietgap",
        expected_source_file="tong-quan-vietgap.md",
        max_answer_chars=420,
    ),
    DemoCase(
        question="ACM là gì?",
        required_keywords=("hệ thống", "quản lý"),
        expected_source_category="acm",
        expected_source_file="tong-quan-he-thong.md",
        max_answer_chars=420,
    ),
    DemoCase(
        question="Làm sao tạo mùa vụ trong hệ thống ACM?",
        required_keywords=("Quản lý mùa vụ", "Tạo mùa vụ", "Lưu"),
        expected_source_category="acm",
        expected_source_file="tao-mua-vu.md",
    ),
    DemoCase(
        question="Sau khi tạo mùa vụ cần làm gì?",
        required_keywords=("nhật ký", "thu hoạch"),
        expected_source_category="acm",
        expected_source_file="tao-mua-vu.md",
    ),
    DemoCase(
        question="Nhật ký sản xuất cần ghi những nội dung gì?",
        required_keywords=("đất", "phân bón", "thuốc BVTV", "thu hoạch"),
        expected_source_category="acm",
        expected_source_file="ghi-nhat-ky-san-xuat.md",
    ),
    DemoCase(
        question="Khi thu hoạch xong thì nhập kho trong ACM như thế nào?",
        required_keywords=("thu hoạch", "nhập kho", "sản lượng"),
        expected_source_category="acm",
        expected_source_file="thu-hoach-va-nhap-kho.md",
    ),
    DemoCase(
        question="Mã QR truy xuất nguồn gốc hiển thị những thông tin nào?",
        required_keywords=("tên sản phẩm", "nông trại", "ngày thu hoạch"),
        expected_source_category="traceability",
        expected_source_file="thong-tin-hien-thi-khi-quet-qr.md",
    ),
    DemoCase(
        question="Người mua liên kết giỏ hàng với nông trại như thế nào?",
        required_keywords=("giỏ hàng", "nông trại", "đặt hàng"),
        expected_source_category="acm",
        expected_source_file="lien-ket-gio-hang-nong-trai.md",
    ),
    DemoCase(
        question="Cà chua thường gặp sâu bệnh nào?",
        required_keywords=("bọ phấn trắng", "sâu đục quả", "héo xanh"),
        expected_source_category="crop",
        expected_source_file="sau-benh-thuong-gap.md",
        forbidden_source_files=("data/crops/rau-an-la/", "data/crops/gao/"),
    ),
    DemoCase(
        question="Hệ thống có hỗ trợ thanh toán blockchain không?",
        exact_fallback=True,
        max_answer_chars=120,
    ),
]


def ollama_available() -> tuple[bool, str]:
    try:
        response = requests.get(f"{settings.OLLAMA_BASE_URL}/api/tags", timeout=5)
        response.raise_for_status()
    except requests.RequestException as exc:
        return False, str(exc)
    return True, "OK"


def source_values(result: dict) -> tuple[str, str]:
    sources = result.get("sources") or []
    file_names = " ".join(str(getattr(source, "file_name", "")) for source in sources)
    categories = " ".join(str(getattr(source, "category", "")) for source in sources)
    # SourceDocument does not expose category, so fall back to snippets/headings
    # for display while file expectations stay exact.
    headings = " ".join(str(getattr(source, "heading", "")) for source in sources)
    snippets = " ".join(str(getattr(source, "snippet", "")) for source in sources)
    return file_names, f"{categories} {headings} {snippets}"


def validate_case(case: DemoCase, result: dict) -> list[str]:
    answer = str(result.get("answer") or "").strip()
    file_names, source_text = source_values(result)
    failures: list[str] = []

    if case.exact_fallback:
        if answer != INSUFFICIENT_DATA_MESSAGE:
            failures.append("fallback mismatch")
        if result.get("sources"):
            failures.append("fallback should not return sources")
        return failures

    lowered_answer = answer.casefold()
    if answer.startswith(case.question):
        failures.append("answer repeats question prefix")
    for phrase in FORBIDDEN_PHRASES:
        if phrase.casefold() in lowered_answer:
            failures.append(f"forbidden phrase: {phrase}")
    for keyword in case.required_keywords:
        if keyword.casefold() not in lowered_answer:
            failures.append(f"missing keyword: {keyword}")
    if len(answer) > case.max_answer_chars:
        failures.append(f"answer too long: {len(answer)} > {case.max_answer_chars}")
    if case.expected_source_file and case.expected_source_file not in file_names:
        failures.append(f"missing source file: {case.expected_source_file}")
    if case.expected_source_category:
        expected_files = CATEGORY_FILE_HINTS.get(case.expected_source_category, set())
        if not any(file_name in file_names for file_name in expected_files):
            failures.append(f"missing source category signal: {case.expected_source_category}")
    for forbidden_source in case.forbidden_source_files:
        if forbidden_source.casefold() in file_names.casefold() or forbidden_source.casefold() in source_text.casefold():
            failures.append(f"forbidden source: {forbidden_source}")

    return failures


def main() -> int:
    available, status = ollama_available()
    print(f"Ollama availability: {'OK' if available else 'UNAVAILABLE'} ({status})")
    if not available:
        print("Live demo checks skipped because Ollama is not reachable.")
        return 2

    rag = RagService()
    failures_total = 0

    for index, case in enumerate(DEMO_CASES, start=1):
        print(f"\n[{index}] {case.question}")
        result = rag.chat(case.question)
        answer = result.get("answer", "")
        print(f"Answer ({len(answer)} chars): {answer}")
        sources = result.get("sources") or []
        if sources:
            print("Sources:")
            for source in sources:
                heading = f" > {source.heading}" if source.heading else ""
                print(f"- {source.file_name}{heading}")
        else:
            print("Sources: none")

        failures = validate_case(case, result)
        if failures:
            failures_total += 1
            print("Status: FAIL - " + "; ".join(failures))
        else:
            print("Status: PASS")

    print(f"\nDemo result: {len(DEMO_CASES) - failures_total}/{len(DEMO_CASES)} passed")
    return 1 if failures_total else 0


if __name__ == "__main__":
    raise SystemExit(main())
