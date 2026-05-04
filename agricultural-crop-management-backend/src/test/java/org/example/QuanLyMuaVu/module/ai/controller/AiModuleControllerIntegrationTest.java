package org.example.QuanLyMuaVu.module.ai.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.QuanLyMuaVu.module.ai.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = { AIController.class, ChatController.class })
class AiModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeminiService geminiService;

    @Test
    @WithMockUser(roles = "FARMER")
    void chat_returnsAssistantReply() throws Exception {
        when(geminiService.chatAsAgriculturalExpert(eq("How to improve soil?"), eq("rice")))
                .thenReturn("Use compost and rotate crops.");

        mockMvc.perform(post("/api/v1/farmer/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userMessage": "How to improve soil?",
                                  "cropContext": "rice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userMessage").value("How to improve soil?"))
                .andExpect(jsonPath("$.result.cropContext").value("rice"))
                .andExpect(jsonPath("$.result.assistantMessage").value("Use compost and rotate crops."));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void suggestions_returnsPayloadForFarmer() throws Exception {
        mockMvc.perform(get("/api/v1/farmer/ai/suggestions")
                        .param("crop", "rice")
                        .param("soil", "loam")
                        .param("season", "summer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.crop").value("rice"))
                .andExpect(jsonPath("$.result.soil").value("loam"))
                .andExpect(jsonPath("$.result.suggestions[0]").value(containsString("Use drip irrigation")));
    }
}
