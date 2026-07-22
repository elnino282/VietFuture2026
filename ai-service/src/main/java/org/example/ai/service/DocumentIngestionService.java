package org.example.ai.service;

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
