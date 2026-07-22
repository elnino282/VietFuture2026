package org.example.ai.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestionRouterTests {

    private final QuestionRouter router = new QuestionRouter();

    @Test
    public void testOffTopicRouting() {
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("Ai tạo ra bạn?"));
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("thời tiết hôm nay thế nào"));
    }

    @Test
    public void testRagRouting() {
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Cách trồng lúa VietGAP"));
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Quy trình bón phân cho thanh long"));
    }
}
