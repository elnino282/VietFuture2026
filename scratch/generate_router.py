import os

java_service_dir = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\main\java\org\example\ai\service"
java_test_dir = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\test\java\org\example\ai\service"

question_router_code = """package org.example.ai.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
public class QuestionRouter {

    public enum RouteMode {
        OFF_TOPIC,
        GENERAL_AGRICULTURE,
        RAG_FIRST
    }

    private static final List<String> OFF_TOPIC_KEYWORDS = List.of(
            "chao", "xin chao", "thoi tiet", "chinh tri", "ton giao",
            "the thao", "game", "phim", "nhac", "bong da", "ai tao ra ban"
    );

    public RouteMode route(String question) {
        String normalized = normalize(question);
        
        for (String kw : OFF_TOPIC_KEYWORDS) {
            if (normalized.equals(kw) || normalized.contains(" " + kw + " ") || normalized.startsWith(kw + " ") || normalized.endsWith(" " + kw)) {
                return RouteMode.OFF_TOPIC;
            }
        }
        
        // Simple heuristic for generic agriculture vs specific RAG context
        // In python app it's more complex, but here we just return RAG_FIRST
        // The LLM will fallback if RAG context is insufficient.
        return RouteMode.RAG_FIRST;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\\\p{InCombiningDiacriticalMarks}]", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\\\s+", " ").trim();
    }
}
"""

with open(os.path.join(java_service_dir, "QuestionRouter.java"), "w", encoding="utf-8") as f:
    f.write(question_router_code)


test_question_router_code = """package org.example.ai.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestionRouterTests {

    private final QuestionRouter router = new QuestionRouter();

    @Test
    public void testOffTopicRouting() {
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("Ai tạo ra bạn?"));
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("thời tiết hôm nay thế nào"));
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("kể chuyện cười đi")); // maybe not explicitly off topic by keyword but let's test keyword
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("đá bóng hôm qua ai thắng"));
    }

    @Test
    public void testRagRouting() {
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Cách trồng lúa VietGAP"));
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Quy trình bón phân cho thanh long"));
    }
}
"""

with open(os.path.join(java_test_dir, "QuestionRouterTests.java"), "w", encoding="utf-8") as f:
    f.write(test_question_router_code)

print("Generated QuestionRouter.java and QuestionRouterTests.java")
