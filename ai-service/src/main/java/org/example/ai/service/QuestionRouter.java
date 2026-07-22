package org.example.ai.service;

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
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
