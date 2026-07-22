import os
import textwrap

java_service_dir = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\main\java\org\example\ai\service"
java_test_dir = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\test\java\org\example\ai\service"

markdown_chunker_code = """package org.example.ai.service;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownChunker {

    private static final Map<String, String> CATEGORY_ALIASES = Map.of(
            "acm", "acm",
            "farmtrace", "acm",
            "vietgap", "vietgap",
            "faq", "faq",
            "crops", "crop",
            "crop", "crop",
            "traceability", "traceability",
            "templates", "template",
            "template", "template"
    );

    private static final Pattern HEADING_RE = Pattern.compile("^(#{1,6})\\\\s+(.+?)\\\\s*$");
    private static final Pattern FENCE_RE = Pattern.compile("^\\\\s*(```|~~~)");

    public static List<Document> buildDocumentChunks(Path filePath, Path dataDir) {
        String text;
        try {
            text = Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }

        Map<String, Object> baseMetadata = buildBaseMetadata(filePath, dataDir);
        List<Document> chunks = new ArrayList<>();

        String[] lines = text.split("\\r?\\n");
        String currentHeading = "Tài liệu";
        List<String> currentLines = new ArrayList<>();
        int headingIndex = 0;
        boolean inCodeFence = false;

        for (String line : lines) {
            if (FENCE_RE.matcher(line).find()) {
                inCodeFence = !inCodeFence;
                currentLines.add(line);
                continue;
            }

            Matcher match = !inCodeFence ? HEADING_RE.matcher(line) : null;
            if (match != null && match.matches()) {
                String body = String.join("\\n", currentLines).trim();
                if (!body.isEmpty()) {
                    Map<String, Object> meta = new HashMap<>(baseMetadata);
                    meta.put("heading", currentHeading);
                    meta.put("chunk_id", baseMetadata.get("category") + ":" + baseMetadata.get("source") + ":" + headingIndex + ":0");
                    chunks.add(new Document(body, meta));
                }
                headingIndex++;
                currentHeading = match.group(2).trim();
                currentLines.clear();
                currentLines.add(line);
                continue;
            }

            currentLines.add(line);
        }

        String body = String.join("\\n", currentLines).trim();
        if (!body.isEmpty()) {
            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("heading", currentHeading);
            meta.put("chunk_id", baseMetadata.get("category") + ":" + baseMetadata.get("source") + ":" + headingIndex + ":0");
            chunks.add(new Document(body, meta));
        }

        if (chunks.isEmpty() && !text.trim().isEmpty()) {
            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("heading", "Tài liệu");
            meta.put("chunk_id", baseMetadata.get("category") + ":" + baseMetadata.get("source") + ":0:0");
            chunks.add(new Document(text.trim(), meta));
        }

        return chunks;
    }

    private static Map<String, Object> buildBaseMetadata(Path filePath, Path dataDir) {
        Map<String, Object> metadata = new HashMap<>();
        String category = "unknown";
        try {
            Path relative = dataDir.relativize(filePath);
            if (relative.getNameCount() > 0) {
                String folder = relative.getName(0).toString().toLowerCase();
                category = CATEGORY_ALIASES.getOrDefault(folder, "unknown");
            }
        } catch (IllegalArgumentException e) {
            // not under dataDir
        }

        metadata.put("category", category);
        
        String source = filePath.toString();
        try {
            source = dataDir.getParent().relativize(filePath).toString().replace("\\\\", "/");
        } catch (Exception ignored) {}
        
        metadata.put("source", source);
        metadata.put("file_name", filePath.getFileName().toString());
        return metadata;
    }
}
"""

with open(os.path.join(java_service_dir, "MarkdownChunker.java"), "w", encoding="utf-8") as f:
    f.write(markdown_chunker_code)


document_ingestion_service_code = """package org.example.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private final VectorStore vectorStore;

    public DocumentIngestionService(org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStore = vectorStoreProvider.getIfAvailable();
    }

    public String ingestData(String dataDir) {
        if (vectorStore == null) {
            return "VectorStore is not configured or available.";
        }

        Path dirPath = Paths.get(dataDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return "Data directory does not exist: " + dirPath.toAbsolutePath();
        }

        List<Document> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".md"))
                 .forEach(path -> {
                     documents.addAll(MarkdownChunker.buildDocumentChunks(path, dirPath));
                 });

            if (documents.isEmpty()) {
                return "No text/markdown documents found to ingest.";
            }

            TokenTextSplitter splitter = new TokenTextSplitter(800, 350, 5, 10000, true);
            List<Document> splitDocuments = splitter.apply(documents);

            vectorStore.add(splitDocuments);

            return String.format("Successfully ingested %d chunks into Chroma.", splitDocuments.size());
        } catch (IOException e) {
            log.error("Failed to walk data directory", e);
            return "Failed to read data directory: " + e.getMessage();
        }
    }
}
"""

with open(os.path.join(java_service_dir, "DocumentIngestionService.java"), "w", encoding="utf-8") as f:
    f.write(document_ingestion_service_code)


test_markdown_chunking_code = """package org.example.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MarkdownChunkingTests {

    @TempDir
    Path tempDir;

    @Test
    public void testMarkdownHeadingChunksPreserveRequiredMetadata() throws Exception {
        Path dataDir = tempDir.resolve("data");
        Path fileDir = dataDir.resolve("vietgap");
        Files.createDirectories(fileDir);
        Path filePath = fileDir.resolve("sample.md");
        
        String content = "# Tai lieu VietGAP\\n\\n" +
                "Mo dau.\\n\\n" +
                "## Nguon nuoc\\n\\n" +
                "Nuoc tuoi can an toan.\\n\\n" +
                "### Kiem tra nuoc\\n\\n" +
                "Can ghi chep ket qua kiem tra.";
        Files.writeString(filePath, content);

        List<Document> chunks = MarkdownChunker.buildDocumentChunks(filePath, dataDir);

        assertTrue(chunks.stream().anyMatch(d -> "Nguon nuoc".equals(d.getMetadata().get("heading"))));
        assertTrue(chunks.stream().anyMatch(d -> "Kiem tra nuoc".equals(d.getMetadata().get("heading"))));

        Document waterChunk = chunks.stream().filter(d -> "Nguon nuoc".equals(d.getMetadata().get("heading"))).findFirst().get();
        assertEquals("vietgap", waterChunk.getMetadata().get("category"));
        assertEquals("data/vietgap/sample.md", waterChunk.getMetadata().get("source"));
        assertEquals("sample.md", waterChunk.getMetadata().get("file_name"));
        assertTrue(waterChunk.getMetadata().get("chunk_id").toString().startsWith("vietgap:data/vietgap/sample.md:"));
        assertTrue(waterChunk.getContent().contains("Nuoc tuoi can an toan"));
    }

    @Test
    public void testUnknownFolderCategoryFallsBackToUnknown() throws Exception {
        Path dataDir = tempDir.resolve("data");
        Path fileDir = dataDir.resolve("misc");
        Files.createDirectories(fileDir);
        Path filePath = fileDir.resolve("note.md");
        Files.writeString(filePath, "## Tieu de\\n\\nNoi dung.");

        List<Document> chunks = MarkdownChunker.buildDocumentChunks(filePath, dataDir);
        assertEquals("unknown", chunks.get(0).getMetadata().get("category"));
    }
}
"""

os.makedirs(java_test_dir, exist_ok=True)
with open(os.path.join(java_test_dir, "MarkdownChunkingTests.java"), "w", encoding="utf-8") as f:
    f.write(test_markdown_chunking_code)

print("Generated MarkdownChunker.java, DocumentIngestionService.java, and MarkdownChunkingTests.java")
