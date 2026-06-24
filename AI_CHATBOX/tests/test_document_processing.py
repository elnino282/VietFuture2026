import tempfile
import unittest
from pathlib import Path

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import settings
from app.services.document_service import DocumentService
from app.services.markdown_chunker import build_document_chunks


class MarkdownChunkingTests(unittest.TestCase):
    def test_markdown_heading_chunks_preserve_required_metadata(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            data_dir = Path(temp_dir) / "data"
            file_path = data_dir / "vietgap" / "sample.md"
            file_path.parent.mkdir(parents=True)
            file_path.write_text(
                "# Tai lieu VietGAP\n\n"
                "Mo dau.\n\n"
                "## Nguon nuoc\n\n"
                "Nuoc tuoi can an toan.\n\n"
                "### Kiem tra nuoc\n\n"
                "Can ghi chep ket qua kiem tra.",
                encoding="utf-8",
            )

            chunks = build_document_chunks(file_path, data_dir)

        headings = [chunk.metadata["heading"] for chunk in chunks]
        self.assertIn("Nguon nuoc", headings)
        self.assertIn("Kiem tra nuoc", headings)

        water_chunk = next(chunk for chunk in chunks if chunk.metadata["heading"] == "Nguon nuoc")
        self.assertEqual(water_chunk.metadata["category"], "vietgap")
        self.assertEqual(water_chunk.metadata["source"], "data/vietgap/sample.md")
        self.assertEqual(water_chunk.metadata["file_name"], "sample.md")
        self.assertTrue(water_chunk.metadata["chunk_id"].startswith("vietgap:data/vietgap/sample.md:"))
        self.assertIn("Nuoc tuoi can an toan", water_chunk.page_content)

    def test_unknown_folder_category_falls_back_to_unknown(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            data_dir = Path(temp_dir) / "data"
            file_path = data_dir / "misc" / "note.md"
            file_path.parent.mkdir(parents=True)
            file_path.write_text("## Tieu de\n\nNoi dung.", encoding="utf-8")

            chunks = build_document_chunks(file_path, data_dir)

        self.assertEqual(chunks[0].metadata["category"], "unknown")

    def test_document_service_resplits_oversized_heading_chunks_with_parent_metadata(self):
        doc = Document(
            page_content=" ".join(f"noi-dung-{index}" for index in range(80)),
            metadata={
                "category": "vietgap",
                "source": "data/vietgap/long.md",
                "file_name": "long.md",
                "heading": "Quy trinh dai",
                "chunk_id": "vietgap:long.md:0:0",
            },
        )
        service = DocumentService.__new__(DocumentService)
        service.splitter = RecursiveCharacterTextSplitter(chunk_size=120, chunk_overlap=20)
        original_chunk_size = settings.CHUNK_SIZE
        settings.CHUNK_SIZE = 60
        try:
            chunks = service._split_oversized_documents([doc])
        finally:
            settings.CHUNK_SIZE = original_chunk_size

        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(chunk.metadata["heading"] == "Quy trinh dai" for chunk in chunks))
        self.assertTrue(all(chunk.metadata["parent_chunk_id"] == "vietgap:long.md:0:0" for chunk in chunks))
        self.assertEqual(chunks[0].metadata["chunk_id"], "vietgap:long.md:0:0:part-0")
        self.assertEqual(chunks[1].metadata["chunk_id"], "vietgap:long.md:0:0:part-1")


if __name__ == "__main__":
    unittest.main()
