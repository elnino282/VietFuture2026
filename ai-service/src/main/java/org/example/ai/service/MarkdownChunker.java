package org.example.ai.service;

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

    private static final Pattern HEADING_RE = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern FENCE_RE = Pattern.compile("^\\s*(```|~~~)");

    public static List<Document> buildDocumentChunks(Path filePath, Path dataDir) {
        String text;
        try {
            text = Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }

        Map<String, Object> baseMetadata = buildBaseMetadata(filePath, dataDir);
        List<Document> chunks = new ArrayList<>();

        String[] lines = text.split("\r?\n");
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
                String body = String.join("\n", currentLines).trim();
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

        String body = String.join("\n", currentLines).trim();
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
            source = dataDir.getParent().relativize(filePath).toString().replace("\\", "/");
        } catch (Exception ignored) {}
        
        metadata.put("source", source);
        metadata.put("file_name", filePath.getFileName().toString());
        return metadata;
    }
}
