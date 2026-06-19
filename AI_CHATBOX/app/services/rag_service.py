import logging
from typing import List

from app.constants import MAX_PROMPT_CHUNK_CHARS
from app.config import settings
from app.prompts.system_prompt import SYSTEM_PROMPT, RAG_PROMPT_TEMPLATE
from app.schemas.chat_schema import SourceDocument
from app.services.ollama_service import OllamaService
from app.services.rag_retrieval import (
    INSUFFICIENT_DATA_MESSAGE,
    RetrievedContext,
    detect_intent,
    expand_query,
    is_insufficient_answer,
    normalize_text,
    select_best_contexts,
)
from app.services.source_sanitizer import sanitize_prompt_content, sanitize_public_snippet
from app.vectorstore.chroma_store import ChromaStore

logger = logging.getLogger(__name__)

_EXTRACTIVE_STOPWORDS = {
    "vietgap",
    "farmtrace",
    "nhu",
    "the",
    "nao",
    "gi",
    "co",
    "can",
    "yeu",
    "cau",
    "khong",
    "hay",
    "cho",
    "toi",
}


class RagService:
    def __init__(self) -> None:
        self.chroma_store = ChromaStore()
        self.ollama_service = OllamaService()

    def _search_candidates(
        self,
        queries: list[str],
        fetch_k: int,
        category: str | None = None,
    ) -> list[RetrievedContext]:
        metadata_filter = {"category": category} if category else None
        candidates: list[RetrievedContext] = []
        for query in queries:
            results = self.chroma_store.similarity_search_with_score(
                query,
                k=fetch_k,
                filter=metadata_filter,
            )
            for doc, score in results:
                candidates.append(RetrievedContext(doc=doc, score=score, query=query))
                logger.info(
                    "[RAG-RETRIEVED] category_filter=%s query=%r score=%.4f file=%s heading=%s chunk_id=%s",
                    category or "all",
                    query,
                    score,
                    doc.metadata.get("file_name") or doc.metadata.get("source"),
                    doc.metadata.get("heading"),
                    doc.metadata.get("chunk_id"),
                )
        return candidates

    def _retrieve_contexts(self, question: str, top_k: int) -> list[RetrievedContext]:
        fetch_k = max(top_k * 4, 12)
        intent = detect_intent(question)
        queries = expand_query(question)
        max_distance = settings.MAX_DISTANCE_THRESHOLD

        if settings.MIN_RETRIEVAL_SCORE is not None:
            logger.info(
                "[RAG] MIN_RETRIEVAL_SCORE=%s is ignored for Chroma distance scores; "
                "using MAX_DISTANCE_THRESHOLD=%s",
                settings.MIN_RETRIEVAL_SCORE,
                max_distance,
            )

        logger.info(
            "[RAG] intent=%s confidence=%s scores=%s expanded_queries=%s fetch_k=%d max_distance=%s",
            intent.category or "all",
            intent.confidence,
            intent.scores,
            queries,
            fetch_k,
            max_distance,
        )

        fallback_used = False
        if intent.confidence == "high" and intent.category:
            filtered_candidates = self._search_candidates(queries, fetch_k, category=intent.category)
            selected = select_best_contexts(filtered_candidates, top_k, max_distance)
            weak_required_count = min(top_k, 2)
            if len(selected) >= weak_required_count:
                logger.info(
                    "[RAG] route=filtered category=%s final_context_count=%d",
                    intent.category,
                    len(selected),
                )
                return selected

            fallback_used = True
            logger.info(
                "[RAG] weak filtered result count=%d required=%d; falling back to all categories",
                len(selected),
                weak_required_count,
            )

        all_candidates = self._search_candidates(queries, fetch_k, category=None)
        selected = select_best_contexts(all_candidates, top_k, max_distance)
        logger.info(
            "[RAG] route=all fallback_used=%s final_context_count=%d",
            fallback_used,
            len(selected),
        )
        return selected

    def _build_context(self, contexts: list[RetrievedContext]) -> str:
        if not contexts:
            return ""

        context_parts: List[str] = []
        total_chars = 0
        for index, context in enumerate(contexts[:5], start=1):
            doc = context.doc
            heading = doc.metadata.get("heading") or "Tài liệu"
            content = sanitize_prompt_content(doc.page_content, max_chars=MAX_PROMPT_CHUNK_CHARS)
            if not content:
                continue
            part = (
                f"[TÀI LIỆU {index}]\n"
                f"Tiêu đề: {heading}\n"
                f"{content}"
            )

            if total_chars + len(part) > settings.MAX_CONTEXT_CHARS:
                remaining = settings.MAX_CONTEXT_CHARS - total_chars
                if remaining > 100:
                    context_parts.append(part[:remaining] + "...")
                break

            context_parts.append(part)
            total_chars += len(part)

        context = "\n\n".join(context_parts)
        logger.info(
            "Built context: %d chars from %d/%d docs",
            len(context),
            len(context_parts),
            len(contexts),
        )
        return context

    def _build_sources(self, contexts: list[RetrievedContext]) -> List[SourceDocument]:
        sources: List[SourceDocument] = []
        seen: set[tuple[str | None, str | None]] = set()
        for context in contexts:
            doc = context.doc
            file_name = self._safe_file_name(doc.metadata)
            heading = doc.metadata.get("heading") or "Tài liệu"
            key = (file_name, heading)
            if key in seen:
                continue
            seen.add(key)

            snippet = sanitize_public_snippet(doc.page_content)
            page = doc.metadata.get("page")
            sources.append(
                SourceDocument(
                    snippet=snippet,
                    file_name=file_name,
                    heading=heading,
                    page=page + 1 if isinstance(page, int) else None,
                )
            )
        return sources

    @staticmethod
    def _safe_file_name(metadata: dict) -> str:
        raw = metadata.get("file_name") or metadata.get("source") or "Tài liệu"
        value = str(raw).replace("\\", "/").rstrip("/")
        if "://" in value:
            value = value.split("?")[0].rstrip("/").split("/")[-1]
        else:
            value = value.split("/")[-1]
        return value or "Tài liệu"

    def _build_extractive_answer(self, question: str, contexts: list[RetrievedContext]) -> str:
        query_terms = {
            term
            for term in normalize_text(question).split()
            if len(term) > 2 and term not in _EXTRACTIVE_STOPWORDS
        }
        candidates: list[tuple[int, int, str]] = []
        fallback_lines: list[str] = []

        for context_index, context in enumerate(contexts):
            lines = context.doc.page_content.replace("\r\n", "\n").split("\n")
            for line_index, raw_line in enumerate(lines):
                line = raw_line.strip().lstrip("#-*0123456789. ").strip()
                if not line or len(line) < 20:
                    continue
                fallback_lines.append(line)
                normalized_line = normalize_text(line)
                score = sum(1 for term in query_terms if term in normalized_line)
                if score:
                    candidates.append((score, context_index * 1000 + line_index, line))

        selected_lines: list[str] = []
        seen: set[str] = set()
        ranked_lines = [
            item[2]
            for item in sorted(candidates, key=lambda item: (-item[0], item[1]))
        ] or fallback_lines

        for line in ranked_lines:
            normalized_line = normalize_text(line)
            if normalized_line in seen:
                continue
            selected_lines.append(line)
            seen.add(normalized_line)
            if len(selected_lines) >= 5:
                break

        if not selected_lines:
            return INSUFFICIENT_DATA_MESSAGE
        return "\n".join(f"- {line}" for line in selected_lines)

    def chat(self, question: str, top_k: int | None = None) -> dict:
        k = top_k or settings.DEFAULT_TOP_K
        logger.info("Chat request: question=%r, top_k=%d", question, k)

        contexts = self._retrieve_contexts(question, k)
        logger.info("Selected %d contexts after routing/filtering", len(contexts))

        if not contexts:
            logger.info("No selected contexts; returning insufficient-data response")
            return {
                "answer": INSUFFICIENT_DATA_MESSAGE,
                "sources": [],
            }

        context = self._build_context(contexts)
        prompt = RAG_PROMPT_TEMPLATE.format(
            system_prompt=SYSTEM_PROMPT.strip(),
            context=context,
            question=question,
        )

        answer = self.ollama_service.generate(prompt, chunks_count=len(contexts))
        if is_insufficient_answer(answer):
            logger.info("Model returned insufficient-data response with selected contexts; using extractive fallback")
            answer = self._build_extractive_answer(question, contexts)
        answer = answer.strip()
        sources = [] if is_insufficient_answer(answer) else self._build_sources(contexts)
        logger.info("Generated answer: %d chars", len(answer))

        return {
            "answer": answer,
            "sources": sources,
        }
