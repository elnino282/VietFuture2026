package org.example.QuanLyMuaVu.module.season.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.example.QuanLyMuaVu.module.ai.dto.request.SeasonCostOptimizationSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostCategoryBreakdown;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSuggestionResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSummaryResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonInventoryUsageSummary;
import org.example.QuanLyMuaVu.module.ai.service.SeasonCostOptimizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SeasonCostOptimizationController.class)
@Import(SeasonCostOptimizationControllerTest.MethodSecurityTestConfig.class)
class SeasonCostOptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeasonCostOptimizationService seasonCostOptimizationService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getSummary_returnsSummaryResponse() throws Exception {
        SeasonCostOptimizationSummaryResponse summary = SeasonCostOptimizationSummaryResponse.builder()
                .seasonId(15)
                .seasonName("Mua vu he thu 2026")
                .budgetAmount(new BigDecimal("12000000"))
                .totalExpense(new BigDecimal("9600000"))
                .remainingBudget(new BigDecimal("2400000"))
                .expenseByCategory(List.of(
                        SeasonCostCategoryBreakdown.builder()
                                .category("FERTILIZER")
                                .amount(new BigDecimal("3500000"))
                                .percentageOfTotal(new BigDecimal("36.46"))
                                .build()))
                .topCostCategories(List.of(
                        SeasonCostCategoryBreakdown.builder()
                                .category("FERTILIZER")
                                .amount(new BigDecimal("3500000"))
                                .percentageOfTotal(new BigDecimal("36.46"))
                                .build()))
                .expectedYieldKg(new BigDecimal("15000"))
                .actualYieldKg(new BigDecimal("13200"))
                .costPerExpectedKg(new BigDecimal("640"))
                .costPerActualKg(new BigDecimal("727.27"))
                .laborCost(new BigDecimal("2100000"))
                .pesticideTreatmentCost(new BigDecimal("1250000"))
                .inventoryUsageSummary(List.of(
                        SeasonInventoryUsageSummary.builder()
                                .itemName("Thuoc A")
                                .unit("lit")
                                .totalOutQuantity(new BigDecimal("10"))
                                .movementCount(3)
                                .build()))
                .warnings(List.of("Ngan sach con lai dang thap (<=10% budget)."))
                .disclaimer("AI chi ho tro quyet dinh tham khao.")
                .build();

        when(seasonCostOptimizationService.getSummary(eq(15), isNull())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/seasons/15/cost-optimization/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.seasonId").value(15))
                .andExpect(jsonPath("$.result.totalExpense").value(9600000))
                .andExpect(jsonPath("$.result.expenseByCategory[0].category").value("FERTILIZER"))
                .andExpect(jsonPath("$.result.inventoryUsageSummary[0].itemName").value("Thuoc A"));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void generateSuggestion_returnsSuggestionResponse() throws Exception {
        SeasonCostOptimizationSuggestionResponse response = SeasonCostOptimizationSuggestionResponse.builder()
                .seasonId(15)
                .seasonName("Mua vu he thu 2026")
                .budgetAmount(new BigDecimal("12000000"))
                .totalExpense(new BigDecimal("9600000"))
                .remainingBudget(new BigDecimal("2400000"))
                .warnings(List.of("Ngan sach con lai dang thap (<=10% budget)."))
                .aiSuggestionText("Goi y toi uu chi phi tham khao.")
                .usedContextSummary(Map.of("seasonId", 15, "expenseRows", 18))
                .generatedAt(LocalDateTime.of(2026, 5, 2, 11, 15))
                .disclaimer("AI chi ho tro quyet dinh tham khao.")
                .build();

        when(seasonCostOptimizationService.generateSuggestion(
                eq(15),
                any(SeasonCostOptimizationSuggestionRequest.class),
                isNull()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/seasons/15/cost-optimization/ai-suggestion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Nen toi uu nhom chi phi nao truoc?",
                                  "includeInventory": true,
                                  "additionalNote": "Gia vat tu vua tang"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.seasonId").value(15))
                .andExpect(jsonPath("$.result.aiSuggestionText").value("Goi y toi uu chi phi tham khao."))
                .andExpect(jsonPath("$.result.usedContextSummary.expenseRows").value(18));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getSummary_forbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/seasons/15/cost-optimization/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void generateSuggestion_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/seasons/15/cost-optimization/ai-suggestion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Nen toi uu nhom chi phi nao?"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void generateSuggestion_rejectsTooLongQuestion() throws Exception {
        String tooLongQuestion = "x".repeat(2001);

        mockMvc.perform(post("/api/v1/seasons/15/cost-optimization/ai-suggestion")
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
