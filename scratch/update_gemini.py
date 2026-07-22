import os

file_path = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\main\java\org\example\ai\service\GeminiService.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
imports = """import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import java.util.List;
import java.util.stream.Collectors;
"""
content = content.replace("import org.springframework.stereotype.Service;", imports + "\nimport org.springframework.stereotype.Service;")

# Inject VectorStore
content = content.replace("private final Environment environment;", "private final Environment environment;\n    private final VectorStore vectorStore;")
content = content.replace("public GeminiService(AppProperties appProperties, Environment environment) {", "public GeminiService(AppProperties appProperties, Environment environment, org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider) {\n        this.vectorStore = vectorStoreProvider.getIfAvailable();")

# Modify chatWithPrompt
old_method = """    private String chatWithPrompt(String userMessage,
                                  String context,
                                  String systemPrompt,
                                  String contextLabel,
                                  String questionLabel,
                                  String connectionFallbackMessage,
                                  String emptyResponseFallbackMessage) {
        Objects.requireNonNull(userMessage, "userMessage must not be null");
        if (userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }

        String requestId = UUID.randomUUID().toString();
        if (!aiEnabled) {
            log.warn("Gemini request skipped because AI is disabled (requestId={}).", requestId);
            return connectionFallbackMessage;
        }

        String prompt = buildPrompt(userMessage, context, systemPrompt, contextLabel, questionLabel);"""

new_method = """    private String chatWithPrompt(String userMessage,
                                  String context,
                                  String systemPrompt,
                                  String contextLabel,
                                  String questionLabel,
                                  String connectionFallbackMessage,
                                  String emptyResponseFallbackMessage) {
        Objects.requireNonNull(userMessage, "userMessage must not be null");
        if (userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }

        String requestId = UUID.randomUUID().toString();
        if (!aiEnabled) {
            log.warn("Gemini request skipped because AI is disabled (requestId={}).", requestId);
            return connectionFallbackMessage;
        }
        
        String combinedContext = context != null ? context : "";
        if (this.vectorStore != null) {
            try {
                List<Document> documents = this.vectorStore.similaritySearch(
                        SearchRequest.query(userMessage).withTopK(5)
                );
                String ragContext = documents.stream()
                        .map(doc -> {
                            String heading = doc.getMetadata().containsKey("heading") ? doc.getMetadata().get("heading").toString() : "Tài liệu";
                            return "[TÀI LIỆU]\\nTiêu đề: " + heading + "\\n" + doc.getContent();
                        })
                        .collect(Collectors.joining("\\n\\n"));
                combinedContext += "\\n\\nThông tin tham khảo từ hệ thống (RAG):\\n" + ragContext;
            } catch (Exception e) {
                log.warn("Failed to retrieve documents from VectorStore for RAG: {}", e.getMessage());
            }
        }

        String prompt = buildPrompt(userMessage, combinedContext, systemPrompt, contextLabel, questionLabel);"""

content = content.replace(old_method, new_method)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("GeminiService updated")
