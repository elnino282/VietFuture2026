import sys
from dataclasses import dataclass
from pathlib import Path

import requests

ROOT_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT_DIR))

from app.config import settings  # noqa: E402
from app.constants import INSUFFICIENT_DATA_MESSAGE, OFF_TOPIC_MESSAGE  # noqa: E402
from app.services.question_router import QuestionRouter  # noqa: E402
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
    expected_mode: str
    required_keywords: tuple[str, ...] = ()
    expected_source_category: str | None = None
    expected_source_file: str | None = None
    forbidden_source_files: tuple[str, ...] = ()
    max_answer_chars: int = 700
    exact_answer: str | None = None
    expect_sources: bool | None = None


DEMO_CASES = [
    DemoCase(
        question="VietGAP là gì?",
        expected_mode="strict_rag",
        required_keywords=("Vietnamese Good Agricultural Practices", "Thực hành nông nghiệp tốt"),
        expected_source_category="vietgap",
        expected_source_file="tong-quan-vietgap.md",
        max_answer_chars=420,
        expect_sources=True,
    ),
    DemoCase(
        question="ACM là gì?",
        expected_mode="strict_rag",
        required_keywords=("hệ thống", "truy xuất"),
        expected_source_category="acm",
        expected_source_file="tong-quan-he-thong.md",
        max_answer_chars=420,
        expect_sources=True,
    ),
    DemoCase(
        question="Sau khi tạo mùa vụ cần làm gì?",
        expected_mode="strict_rag",
        required_keywords=("nhật ký", "thu hoạch"),
        expected_source_category="acm",
        expected_source_file="tao-mua-vu.md",
        expect_sources=True,
    ),
    DemoCase(
        question="Nhật ký sản xuất cần ghi những nội dung gì?",
        expected_mode="strict_rag",
        required_keywords=("đất", "phân bón", "thuốc BVTV", "thu hoạch"),
        expected_source_category="acm",
        expected_source_file="ghi-nhat-ky-san-xuat.md",
        expect_sources=True,
    ),
    DemoCase(
        question="Khi thu hoạch xong thì nhập kho trong ACM như thế nào?",
        expected_mode="strict_rag",
        required_keywords=("thu hoạch", "nhập kho", "sản lượng"),
        expected_source_category="acm",
        expected_source_file="thu-hoach-va-nhap-kho.md",
        expect_sources=True,
    ),
    DemoCase(
        question="Mã QR truy xuất nguồn gốc hiển thị những thông tin nào?",
        expected_mode="strict_rag",
        required_keywords=("tên sản phẩm", "nông trại", "ngày thu hoạch"),
        expected_source_category="traceability",
        expected_source_file="thong-tin-hien-thi-khi-quet-qr.md",
        expect_sources=True,
    ),
    DemoCase(
        question="Người mua liên kết giỏ hàng với nông trại như thế nào?",
        expected_mode="strict_rag",
        required_keywords=("giỏ hàng", "nông trại", "đặt hàng"),
        expected_source_category="acm",
        expected_source_file="lien-ket-gio-hang-nong-trai.md",
        expect_sources=True,
    ),
    DemoCase(
        question="Cà chua thường gặp sâu bệnh nào?",
        expected_mode="rag_first",
        required_keywords=("bọ phấn trắng", "sâu đục quả", "héo xanh"),
        expected_source_category="crop",
        expected_source_file="sau-benh-thuong-gap.md",
        forbidden_source_files=("data/crops/rau-an-la/", "data/crops/gao/"),
        expect_sources=True,
    ),
    DemoCase(
        question="Hệ thống có hỗ trợ thanh toán blockchain không?",
        expected_mode="strict_rag",
        exact_answer=INSUFFICIENT_DATA_MESSAGE,
        max_answer_chars=120,
        expect_sources=False,
    ),
    DemoCase(
        question="VietGAP yêu cầu pH đất chính xác bao nhiêu?",
        expected_mode="strict_rag",
        exact_answer=INSUFFICIENT_DATA_MESSAGE,
        max_answer_chars=120,
        expect_sources=False,
    ),
    DemoCase(
        question="Cà chua bị vàng lá do đâu?",
        expected_mode="general_agriculture_llm",
        required_keywords=("tham khảo chung",),
        expect_sources=False,
    ),
    DemoCase(
        question="Cây thiếu đạm có biểu hiện gì?",
        expected_mode="general_agriculture_llm",
        required_keywords=("tham khảo chung",),
        expect_sources=False,
    ),
    DemoCase(
        question="Sâu xanh ăn lá là gì?",
        expected_mode="general_agriculture_llm",
        required_keywords=("tham khảo chung",),
        expect_sources=False,
    ),
    DemoCase(
        question="Đất bị chai cứng thì nên cải tạo như thế nào?",
        expected_mode="general_agriculture_llm",
        required_keywords=("tham khảo chung",),
        expect_sources=False,
    ),
    DemoCase(
        question="Cây con sau khi trồng cần chăm sóc ra sao?",
        expected_mode="general_agriculture_llm",
        required_keywords=("tham khảo chung",),
        expect_sources=False,
    ),
    DemoCase(
        question="Viết bài thơ về tình yêu.",
        expected_mode="off_topic",
        exact_answer=OFF_TOPIC_MESSAGE,
        expect_sources=False,
    ),
    DemoCase(
        question="Bitcoin hôm nay giá bao nhiêu?",
        expected_mode="off_topic",
        exact_answer=OFF_TOPIC_MESSAGE,
        expect_sources=False,
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
    headings = " ".join(str(getattr(source, "heading", "")) for source in sources)
    snippets = " ".join(str(getattr(source, "snippet", "")) for source in sources)
    return file_names, f"{headings} {snippets}"


def validate_case(case: DemoCase, result: dict, mode: str) -> list[str]:
    answer = str(result.get("answer") or "").strip()
    file_names, source_text = source_values(result)
    sources = result.get("sources") or []
    failures: list[str] = []

    if mode != case.expected_mode:
        failures.append(f"route mismatch: {mode} != {case.expected_mode}")
    if case.exact_answer is not None and answer != case.exact_answer:
        failures.append("exact answer mismatch")
    if case.expect_sources is True and not sources:
        failures.append("expected sources")
    if case.expect_sources is False and sources:
        failures.append("expected no sources")
    if answer.startswith(case.question):
        failures.append("answer repeats question prefix")
    for phrase in FORBIDDEN_PHRASES:
        if phrase.casefold() in answer.casefold():
            failures.append(f"forbidden phrase: {phrase}")
    for keyword in case.required_keywords:
        if keyword.casefold() not in answer.casefold():
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
    router = QuestionRouter()
    failures_total = 0

    for index, case in enumerate(DEMO_CASES, start=1):
        route = router.route(case.question)
        print(f"\n[{index}] {case.question}")
        if settings.DEBUG_RAG:
            print(f"Route: {route.mode} category={route.category} confidence={route.confidence} reason={route.reason}")

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

        failures = validate_case(case, result, route.mode)
        if failures:
            failures_total += 1
            print("Status: FAIL - " + "; ".join(failures))
        else:
            print("Status: PASS")

    print(f"\nDemo result: {len(DEMO_CASES) - failures_total}/{len(DEMO_CASES)} passed")
    return 1 if failures_total else 0


if __name__ == "__main__":
    raise SystemExit(main())
