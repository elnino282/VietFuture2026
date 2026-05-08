package org.example.QuanLyMuaVu.module.season.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import org.example.QuanLyMuaVu.module.ai.dto.request.DiseaseSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.DiseaseSuggestionResponse;
import org.example.QuanLyMuaVu.module.ai.service.DiseaseSuggestionService;
import org.example.QuanLyMuaVu.module.season.service.DiseaseRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DiseaseRecordController.class)
@Import(DiseaseRecordControllerAiSuggestionTest.MethodSecurityTestConfig.class)
class DiseaseRecordControllerAiSuggestionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiseaseRecordService diseaseRecordService;

    @MockBean
    private DiseaseSuggestionService diseaseSuggestionService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void generateAiSuggestion_returnsSuggestionResponse() throws Exception {
        DiseaseSuggestionResponse response = DiseaseSuggestionResponse.builder()
                .diseaseRecordId(42)
                .suggestionText("Goi y xu ly tham khao.")
                .usedContextSummary(Map.of("seasonId", 11, "inventoryRows", 4))
                .generatedAt(LocalDateTime.of(2026, 5, 2, 9, 30))
                .warning("AI chi ho tro quyet dinh tham khao.")
                .build();

        when(diseaseSuggestionService.generateSuggestion(eq(42), any(DiseaseSuggestionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/disease-records/42/ai-suggestion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Toi nen xu ly tiep the nao?",
                                  "includeInventory": true,
                                  "additionalNote": "Da phun 1 lan tuan truoc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.diseaseRecordId").value(42))
                .andExpect(jsonPath("$.result.suggestionText").value("Goi y xu ly tham khao."))
                .andExpect(jsonPath("$.result.usedContextSummary.seasonId").value(11))
                .andExpect(jsonPath("$.result.usedContextSummary.inventoryRows").value(4))
                .andExpect(jsonPath("$.result.warning").value("AI chi ho tro quyet dinh tham khao."));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createDiseaseRecord_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/seasons/11/disease-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diseaseName": "Dao on la",
                                  "severity": "MEDIUM",
                                  "detectedAt": "2026-05-02T09:00:00"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void generateAiSuggestion_rejectsTooLongQuestion() throws Exception {
        String tooLongQuestion = "x".repeat(2001);

        mockMvc.perform(post("/api/v1/disease-records/42/ai-suggestion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "%s"
                                }
                                """.formatted(tooLongQuestion)))
                .andExpect(status().isBadRequest());
    }
}
