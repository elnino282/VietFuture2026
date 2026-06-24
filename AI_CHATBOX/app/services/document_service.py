from collections import defaultdict
from pathlib import Path
import logging
import uuid

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import settings
from app.services.rag_quality import analyze_document_quality
from app.utils.file_loader import load_documents
from app.vectorstore.chroma_store import ChromaStore

logger = logging.getLogger(__name__)

EXCLUDED_FILES = {
    "README.md",
    "data_guide.md",
    "sources.jsonl",
}


class DocumentService:
    def __init__(self) -> None:
        self.chroma_store = ChromaStore()
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.CHUNK_SIZE,
            chunk_overlap=settings.CHUNK_OVERLAP,
        )

    @staticmethod
    def _filter_excluded_documents(documents: list) -> list:
        filtered = []
        for doc in documents:
            file_name = doc.metadata.get("file_name")
            source = doc.metadata.get("source", "")
            if file_name in EXCLUDED_FILES:
                continue
            if Path(str(source)).name in EXCLUDED_FILES:
                continue
            filtered.append(doc)
        return filtered

    @staticmethod
    def _document_source(doc) -> str:
        source = doc.metadata.get("source")
        if source:
            return str(source)
        file_name = doc.metadata.get("file_name")
        if file_name:
            return str(file_name)
        return "unknown"

    @staticmethod
    def _ensure_chunk_metadata(documents: list) -> list:
        """Assign a stable chunk_id to documents that do not already have one.

        Indexing is done per source path instead of globally, so a file's
        chunk_ids stay stable across separate ingest() runs as long as that
        file's chunking stays stable. This also avoids collisions when two
        different folders contain the same file name.
        """
        chunks = []
        per_source_counter: dict[tuple[str, str], int] = defaultdict(int)

        for doc in documents:
            if doc.metadata.get("chunk_id"):
                chunks.append(doc)
                continue

            category = doc.metadata.get("category", "unknown")
            source = DocumentService._document_source(doc)
            page = doc.metadata.get("page")
            page_part = f":page-{page}" if page is not None else ""
            key = (str(category), source)
            index = per_source_counter[key]
            per_source_counter[key] += 1

            doc.metadata.setdefault("heading", "Tài liệu")
            doc.metadata["source"] = source
            doc.metadata.setdefault("file_name", Path(source).name)
            doc.metadata["chunk_id"] = f"{category}:{source}{page_part}:{index}"
            chunks.append(doc)

        return chunks

    def _split_oversized_documents(self, documents: list) -> list:
        chunks = []
        max_unsplit_chars = int(settings.CHUNK_SIZE * 1.5)

        for doc in documents:
            if len(doc.page_content) <= max_unsplit_chars:
                chunks.append(doc)
                continue

            old_chunk_id = doc.metadata.get("chunk_id")
            split_docs = self.splitter.split_documents([doc])

            for index, split_doc in enumerate(split_docs):
                metadata = dict(doc.metadata)
                if old_chunk_id:
                    metadata["parent_chunk_id"] = old_chunk_id
                    metadata["chunk_id"] = f"{old_chunk_id}:part-{index}"
                else:
                    # Defensive fallback only -- every document reaching this
                    # point should already carry a chunk_id from
                    # _ensure_chunk_metadata or the loader. A random suffix
                    # keeps two such documents from colliding on the same id.
                    metadata["chunk_id"] = f"chunk:{uuid.uuid4().hex[:8]}:part-{index}"

                split_doc.metadata = metadata
                chunks.append(split_doc)

        return chunks

    @staticmethod
    def _check_chunk_id_collisions(chunks: list) -> None:
        """Warn if any two chunks ended up with the same chunk_id."""
        seen: dict[str, int] = defaultdict(int)
        for chunk in chunks:
            seen[str(chunk.metadata.get("chunk_id", ""))] += 1

        duplicates = {chunk_id: count for chunk_id, count in seen.items() if count > 1}
        if duplicates:
            logger.warning(
                "[INGEST] %d chunk_id value(s) are duplicated within this batch; "
                "later chunks may overwrite earlier ones in Chroma. Examples: %s",
                len(duplicates),
                list(duplicates.items())[:5],
            )

    @staticmethod
    def _filter_low_value_chunks(chunks: list) -> tuple[list, int]:
        filtered = []
        low_value_count = 0

        for chunk in chunks:
            quality = analyze_document_quality(chunk)
            if quality.is_low_value:
                low_value_count += 1
                logger.debug(
                    "[INGEST-FILTER] drop low-value chunk_id=%s file=%s heading=%s reasons=%s chars=%d",
                    chunk.metadata.get("chunk_id"),
                    chunk.metadata.get("file_name") or chunk.metadata.get("source"),
                    chunk.metadata.get("heading"),
                    ",".join(quality.reasons),
                    len(quality.clean_text),
                )
                continue
            filtered.append(chunk)

        return filtered, low_value_count

    def ingest(self, data_dir: str | None = None, reset: bool = False) -> dict:
        target_dir = Path(data_dir) if data_dir else settings.DATA_DIR
        if not target_dir.is_absolute():
            target_dir = settings.ROOT_DIR / target_dir

        if reset:
            self.chroma_store.reset()

        documents, files_scanned = load_documents(target_dir)
        documents = self._filter_excluded_documents(documents)

        loaded_sources = {
            self._document_source(doc)
            for doc in documents
            if self._document_source(doc) and self._document_source(doc) != "unknown"
        }
        loaded_file_names = {
            str(doc.metadata.get("file_name") or Path(self._document_source(doc)).name)
            for doc in documents
            if doc.metadata.get("file_name") or self._document_source(doc) != "unknown"
        }

        files_excluded_or_skipped = max(files_scanned - len(loaded_sources), 0)

        # Make re-ingestion idempotent at file/source granularity: drop any
        # chunks already indexed for these source paths before adding the
        # freshly-built chunks. Skipped on full reset because reset() already
        # wiped the whole collection.
        if not reset:
            self.chroma_store.delete_by_sources(loaded_sources)

        heading_chunks = [doc for doc in documents if doc.metadata.get("chunk_id")]
        fallback_documents = [doc for doc in documents if not doc.metadata.get("chunk_id")]
        fallback_chunks = self.splitter.split_documents(fallback_documents)
        fallback_chunks = self._ensure_chunk_metadata(fallback_chunks)

        chunks_before_filter = self._split_oversized_documents(heading_chunks + fallback_chunks)
        chunks, low_value_chunks_filtered = self._filter_low_value_chunks(chunks_before_filter)
        self._check_chunk_id_collisions(chunks)

        self.chroma_store.add_documents(chunks)

        logger.info(
            "[INGEST] files_loaded=%d files_excluded_or_skipped=%d heading_chunks=%d "
            "fallback_chunks=%d chunks_before_filter=%d low_value_chunks_filtered=%d "
            "chunks_indexed=%d collection=%s reset=%s",
            len(loaded_sources),
            files_excluded_or_skipped,
            len(heading_chunks),
            len(fallback_chunks),
            len(chunks_before_filter),
            low_value_chunks_filtered,
            len(chunks),
            settings.COLLECTION_NAME,
            reset,
        )

        return {
            "files_loaded": len(loaded_sources),
            "files_excluded": files_excluded_or_skipped,
            "files_excluded_or_skipped": files_excluded_or_skipped,
            "file_names_loaded": sorted(loaded_file_names),
            "sources_loaded": sorted(loaded_sources),
            "heading_chunks": len(heading_chunks),
            "fallback_chunks": len(fallback_chunks),
            "chunks_before_filter": len(chunks_before_filter),
            "low_value_chunks_filtered": low_value_chunks_filtered,
            "chunks_indexed": len(chunks),
            "collection_name": settings.COLLECTION_NAME,
        }
