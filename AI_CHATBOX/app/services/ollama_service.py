import logging
import re
import time

import requests
from langchain_ollama import ChatOllama

from app.config import settings
from app.constants import INSUFFICIENT_DATA_MESSAGE
from app.prompts.system_prompt import GENERAL_AGRICULTURE_PROMPT

logger = logging.getLogger(__name__)

_THINK_TAG_RE = re.compile(r"<think>.*?</think>", re.DOTALL | re.IGNORECASE)
_THINK_OPEN_RE = re.compile(r"<think>.*", re.DOTALL | re.IGNORECASE)
_THINK_TAG_ONLY_RE = re.compile(r"</?think>", re.IGNORECASE)
_THINKING_BLOCK_RE = re.compile(
    r"(?:^|\n)\s*Thinking(?:\s*Process)?[:\.]?\s*\n.*?(?=\n\S|\Z)",
    re.DOTALL,
)
_THINKING_LINE_RE = re.compile(r"^\s*Thinking\.{3,}\s*$", re.MULTILINE)
_GENERAL_MAX_ANSWER_CHARS = 650
_GENERAL_GUIDANCE_PREFIX = (
    "Đây là thông tin tham khảo chung; cần kiểm tra thực tế cây trồng trước khi xử lý."
)
_SOURCE_CONTEXT_PREFIX_RE = re.compile(
    r"^\s*(?:"
    r"theo\s+(?:tai\s+lieu|tài\s+liệu)|"
    r"dua\s+tren\s+(?:ngu\s+canh|context)(?:\s+va\s+source\s+noi\s+bo)?|"
    r"dựa\s+trên\s+(?:ngữ\s+cảnh|context)(?:\s+và\s+source\s+nội\s+bộ)?|"
    r"based\s+on\s+(?:the\s+)?(?:context|source)"
    r")\s*[,:\-]*\s*",
    re.IGNORECASE,
)
_SOURCE_CONTEXT_TOKEN_RE = re.compile(
    r"\b(?:source|context|ngu\s+canh|ngữ\s+cảnh|tai\s+lieu|tài\s+liệu)\b",
    re.IGNORECASE,
)
_VIETGAP_COMPLIANCE_RE = re.compile(
    r"(?:chac\s+chan\s+)?(?:dat|đạt|dap\s+ung|đáp\s+ứng|tuan\s+thu|tuân\s+thủ)\s+vietgap",
    re.IGNORECASE,
)
_PESTICIDE_SPECIFIC_RE = re.compile(
    r"\b(?:confidor|regent|actara|antracol|ridomil|score)\b|"
    r"\b\d+(?:[,.]\d+)?\s*(?:ml|l|g|kg)\s*/\s*(?:ha|lit|l|binh|bình)\b|"
    r"\bphun\s+(?:co\s+dinh|cố\s+định)?\s*(?:moi|mỗi)?\s*\d+\s*ngay\b",
    re.IGNORECASE,
)
_SENTENCE_SPLIT_RE = re.compile(r"(?<=[.!?。])\s+")


class OllamaService:
    def __init__(self) -> None:
        self.llm = ChatOllama(
            model=settings.OLLAMA_MODEL,
            base_url=settings.OLLAMA_BASE_URL,
            temperature=settings.TEMPERATURE,
            num_predict=settings.NUM_PREDICT,
            num_ctx=settings.NUM_CTX,
            top_k=settings.TOP_K,
            top_p=settings.TOP_P,
            repeat_penalty=settings.REPEAT_PENALTY,
            keep_alive=settings.KEEP_ALIVE,
            reasoning=False,
            extra_body={"think": settings.OLLAMA_THINK},
        )

    @staticmethod
    def _clean_thinking(text: str) -> str:
        cleaned = _THINK_TAG_RE.sub("", text)
        cleaned = _THINK_OPEN_RE.sub("", cleaned)
        cleaned = _THINK_TAG_ONLY_RE.sub("", cleaned)
        cleaned = _THINKING_BLOCK_RE.sub("", cleaned)
        cleaned = _THINKING_LINE_RE.sub("", cleaned)
        return cleaned.strip()

    @staticmethod
    def _build_llm(num_predict: int) -> ChatOllama:
        return ChatOllama(
            model=settings.OLLAMA_MODEL,
            base_url=settings.OLLAMA_BASE_URL,
            temperature=settings.TEMPERATURE,
            num_predict=num_predict,
            num_ctx=settings.NUM_CTX,
            top_k=settings.TOP_K,
            top_p=settings.TOP_P,
            repeat_penalty=settings.REPEAT_PENALTY,
            keep_alive=settings.KEEP_ALIVE,
            reasoning=False,
            extra_body={"think": settings.OLLAMA_THINK},
        )

    @staticmethod
    def _clean_general_answer(text: str, question: str) -> str:
        answer = re.sub(r"\n{3,}", "\n\n", text).strip()
        question = question.strip()
        if question:
            answer = re.sub(
                rf"^\s*(?:Câu hỏi\s*:\s*)?{re.escape(question)}\s*[:：\-–—]?\s*",
                "",
                answer,
                flags=re.IGNORECASE,
            ).strip()
        forbidden_prefixes = [
            r"theo tài liệu(?: được cung cấp)?[,:\s]*",
            r"dựa trên (?:ngữ cảnh|tài liệu)(?: được cung cấp)?[,:\s]*",
            r"nội dung hỗ trợ(?: cho biết)?[,:\s]*",
        ]
        for pattern in forbidden_prefixes:
            answer = re.sub(pattern, "", answer, flags=re.IGNORECASE).strip()
        answer = _SOURCE_CONTEXT_PREFIX_RE.sub("", answer).strip()
        answer = _SOURCE_CONTEXT_TOKEN_RE.sub("", answer).strip(" ,;:-")
        answer = OllamaService._soften_general_safety_claims(answer)
        return answer.replace(
            "Viện Good Agricultural Practices",
            "Vietnamese Good Agricultural Practices",
        ).strip()

    @staticmethod
    def _soften_general_safety_claims(text: str) -> str:
        sentences = [part.strip() for part in _SENTENCE_SPLIT_RE.split(text) if part.strip()]
        if not sentences and text.strip():
            sentences = [text.strip()]

        cleaned: list[str] = []
        added_vietgap_caveat = False
        added_pesticide_caveat = False
        for sentence in sentences:
            if _VIETGAP_COMPLIANCE_RE.search(sentence):
                if not added_vietgap_caveat:
                    cleaned.append("Khong the xac nhan tuan thu VietGAP neu khong co du lieu chung nhan hop le.")
                    added_vietgap_caveat = True
                continue
            if _PESTICIDE_SPECIFIC_RE.search(sentence):
                if not added_pesticide_caveat:
                    cleaned.append(
                        "Voi thuoc BVTV, can doc nhan san pham va hoi can bo ky thuat truoc khi quyet dinh ten thuoc, lieu dung hoac lich phun."
                    )
                    added_pesticide_caveat = True
                continue
            cleaned.append(sentence)

        return " ".join(cleaned).strip()

    @staticmethod
    def _finalize_general_answer(text: str) -> str:
        answer = text.strip()
        if not answer:
            return ""
        if "tham khảo chung" not in answer.casefold():
            answer = f"{_GENERAL_GUIDANCE_PREFIX}\n\n{answer}"
        answer = re.sub(r"(?:\n|\s)*(?:[-*]|\d+[\.)])\s*$", "", answer).rstrip()
        if len(answer) <= _GENERAL_MAX_ANSWER_CHARS:
            return answer

        window = answer[:_GENERAL_MAX_ANSWER_CHARS].rstrip()
        boundaries = [
            window.rfind("\n"),
            window.rfind(". "),
            window.rfind("。"),
            window.rfind("! "),
            window.rfind("? "),
        ]
        boundary = max(boundaries)
        if boundary >= 220:
            trimmed = window[: boundary + 1].rstrip()
        else:
            trimmed = window.rstrip(" ,;:-") + "."
        return re.sub(r"(?:\n|\s)*(?:[-*]|\d+[\.)])\s*$", "", trimmed).rstrip()

    def _fallback_generate(self, prompt: str, num_predict: int | None = None) -> str:
        logger.warning("ChatOllama returned empty content, using /api/chat fallback")
        url = f"{settings.OLLAMA_BASE_URL}/api/chat"
        effective_num_predict = num_predict or settings.NUM_PREDICT
        payload = {
            "model": settings.OLLAMA_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
            "think": settings.OLLAMA_THINK,
            "options": {
                "temperature": settings.TEMPERATURE,
                "num_predict": effective_num_predict,
                "num_ctx": settings.NUM_CTX,
                "top_k": settings.TOP_K,
                "top_p": settings.TOP_P,
                "repeat_penalty": settings.REPEAT_PENALTY,
            },
            "keep_alive": settings.KEEP_ALIVE,
        }
        logger.info(
            "[OLLAMA-FALLBACK] payload: model=%s think=%s ctx=%d predict=%d",
            payload["model"],
            payload["think"],
            payload["options"]["num_ctx"],
            payload["options"]["num_predict"],
        )
        try:
            resp = requests.post(url, json=payload, timeout=120)
            resp.raise_for_status()
            data = resp.json()
        except (requests.RequestException, ValueError):
            logger.exception("[OLLAMA-FALLBACK] request or JSON parsing failed")
            return INSUFFICIENT_DATA_MESSAGE

        content = data.get("message", {}).get("content", "")
        return self._clean_thinking(content) or INSUFFICIENT_DATA_MESSAGE

    def generate(
        self,
        prompt: str,
        chunks_count: int = 0,
        num_predict: int | None = None,
    ) -> str:
        effective_num_predict = num_predict or settings.NUM_PREDICT
        logger.info(
            "[OLLAMA] model=%s think=%s ctx=%d predict=%d chunks=%d prompt_chars=%d",
            settings.OLLAMA_MODEL,
            settings.OLLAMA_THINK,
            settings.NUM_CTX,
            effective_num_predict,
            chunks_count,
            len(prompt),
        )

        start = time.perf_counter()
        try:
            llm = self.llm if effective_num_predict == settings.NUM_PREDICT else self._build_llm(effective_num_predict)
            response = llm.invoke(prompt)
            elapsed_ms = (time.perf_counter() - start) * 1000
            content = self._clean_thinking(response.content or "")
        except Exception:
            elapsed_ms = (time.perf_counter() - start) * 1000
            logger.exception("[OLLAMA] ChatOllama invoke failed after %.0f ms", elapsed_ms)
            return self._fallback_generate(prompt, num_predict=effective_num_predict)

        logger.info(
            "[OLLAMA] latency_ms=%.0f response_chars=%d empty=%s",
            elapsed_ms,
            len(content),
            not bool(content),
        )

        if content:
            return content

        return self._fallback_generate(prompt, num_predict=effective_num_predict) or INSUFFICIENT_DATA_MESSAGE

    def generate_general_agriculture_answer(self, question: str) -> str:
        prompt = GENERAL_AGRICULTURE_PROMPT.format(question=question)
        answer = self.generate(
            prompt,
            chunks_count=0,
            num_predict=settings.GENERAL_AGRICULTURE_MAX_TOKENS,
        )
        if answer == INSUFFICIENT_DATA_MESSAGE:
            return answer
        cleaned = self._clean_general_answer(answer, question=question)
        cleaned = self._finalize_general_answer(cleaned)
        return cleaned or INSUFFICIENT_DATA_MESSAGE
