import os

file_path = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\main\java\org\example\ai\service\GeminiService.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Inject QuestionRouter
content = content.replace("private final VectorStore vectorStore;", "private final VectorStore vectorStore;\n    private final QuestionRouter questionRouter;")
content = content.replace("public GeminiService(AppProperties appProperties, Environment environment, org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider) {", "public GeminiService(AppProperties appProperties, Environment environment, org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider, QuestionRouter questionRouter) {\n        this.questionRouter = questionRouter;")

# Modify chatWithPrompt
old_method_start = """    private String chatWithPrompt(String userMessage,
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
        
        String combinedContext = context != null ? context : "";"""

new_method_start = """    private String chatWithPrompt(String userMessage,
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

        if (this.questionRouter != null && this.questionRouter.route(userMessage) == QuestionRouter.RouteMode.OFF_TOPIC) {
            return "Xin lỗi, tôi chỉ có thể trả lời các câu hỏi liên quan đến nông nghiệp và nền tảng quản lý mùa vụ.";
        }

        String requestId = UUID.randomUUID().toString();
        if (!aiEnabled) {
            log.warn("Gemini request skipped because AI is disabled (requestId={}).", requestId);
            return connectionFallbackMessage;
        }
        
        String combinedContext = context != null ? context : "";"""

content = content.replace(old_method_start, new_method_start)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("GeminiService updated with QuestionRouter")
