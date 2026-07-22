import os

file_path = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\test\java\org\example\ai\config\OpenApiGeneratorTest.java"
with open(file_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith("import "):
        new_lines.append("import org.springframework.boot.test.mock.mockito.MockBean;\n")
        new_lines.append("import org.springframework.ai.vectorstore.VectorStore;\n")
        new_lines.extend(lines[lines.index(line):])
        break
    new_lines.append(line)

content = "".join(new_lines)
content = content.replace("@MockBean(VectorStore.class)\npublic class OpenApiGeneratorTest {", "public class OpenApiGeneratorTest {")
content = content.replace("public class OpenApiGeneratorTest {", "@MockBean(VectorStore.class)\npublic class OpenApiGeneratorTest {")
# Clean up multiple inserts if they happened
content = content.replace("import org.springframework.boot.test.mock.mockito.MockBean;\nimport org.springframework.ai.vectorstore.VectorStore;\nimport org.springframework.boot.test.mock.mockito.MockBean;\nimport org.springframework.ai.vectorstore.VectorStore;", "import org.springframework.boot.test.mock.mockito.MockBean;\nimport org.springframework.ai.vectorstore.VectorStore;")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)


test_path = r"c:\Users\thong\Desktop\VietFuture2026\ai-service\src\test\java\org\example\ai\service\QuestionRouterTests.java"
test_content = """package org.example.ai.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestionRouterTests {

    private final QuestionRouter router = new QuestionRouter();

    @Test
    public void testOffTopicRouting() {
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("Ai tạo ra bạn?"));
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("thời tiết hôm nay thế nào"));
        assertEquals(QuestionRouter.RouteMode.OFF_TOPIC, router.route("đá bóng hôm qua ai thắng"));
    }

    @Test
    public void testRagRouting() {
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Cách trồng lúa VietGAP"));
        assertEquals(QuestionRouter.RouteMode.RAG_FIRST, router.route("Quy trình bón phân cho thanh long"));
    }
}
"""

with open(test_path, "w", encoding="utf-8") as f:
    f.write(test_content)

print("Tests fixed")
