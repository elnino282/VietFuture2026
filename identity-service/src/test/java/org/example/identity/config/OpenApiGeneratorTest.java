package org.example.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiGeneratorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void generateOpenApiSpec() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] yamlBytes = result.getResponse().getContentAsByteArray();

        File outputDir = new File("../docs/openapi");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        Files.write(Paths.get("../docs/openapi/identity-service-v1.yaml"), yamlBytes);
        System.out.println("--- OpenAPI YAML spec successfully exported to docs/openapi/identity-service-v1.yaml ---");
    }
}
