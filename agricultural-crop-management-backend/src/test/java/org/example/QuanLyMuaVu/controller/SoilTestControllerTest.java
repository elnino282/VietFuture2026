package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.module.sustainability.controller.SoilTestController;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateSoilTestRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SoilTestResponse;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.module.sustainability.service.SoilTestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SoilTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class SoilTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SoilTestService soilTestService;

    @Test
    @DisplayName("POST soil-test returns stable ApiResponse contract")
    void create_ReturnsContractShape() throws Exception {
        CreateSoilTestRequest request = CreateSoilTestRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .soilOrganicMatterPct(new BigDecimal("2.1000"))
                .mineralNKgPerHa(new BigDecimal("14.5000"))
                .nitrateMgPerKg(new BigDecimal("12.4000"))
                .ammoniumMgPerKg(new BigDecimal("2.3000"))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("soil-lab-0318.pdf")
                .labReference("LAB-SOIL-21")
                .note("Before heading stage")
                .build();

        SoilTestResponse response = SoilTestResponse.builder()
                .id(201)
                .seasonId(33)
                .plotId(22)
                .plotName("Plot A")
                .sampleDate(LocalDate.of(2026, 3, 18))
                .soilOrganicMatterPct(new BigDecimal("2.1000"))
                .mineralNKgPerHa(new BigDecimal("14.5000"))
                .nitrateMgPerKg(new BigDecimal("12.4000"))
                .ammoniumMgPerKg(new BigDecimal("2.3000"))
                .estimatedNContributionKg(new BigDecimal("29.0000"))
                .status("measured")
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .build();

        when(soilTestService.create(eq(33), any(CreateSoilTestRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/seasons/33/soil-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(201))
                .andExpect(jsonPath("$.result.status").value("measured"))
                .andExpect(jsonPath("$.result.estimatedNContributionKg").value(29.0000))
                .andExpect(jsonPath("$.result.sourceType").value("lab_measured"));
    }

    @Test
    @DisplayName("GET soil-tests returns list contract")
    void list_ReturnsListContractShape() throws Exception {
        when(soilTestService.list(33, 22))
                .thenReturn(List.of(
                        SoilTestResponse.builder()
                                .id(31)
                                .seasonId(33)
                                .plotId(22)
                                .sampleDate(LocalDate.of(2026, 3, 18))
                                .mineralNKgPerHa(new BigDecimal("14.5000"))
                                .status("measured")
                                .sourceType(NutrientInputSourceType.USER_ENTERED)
                                .build()
                ));

        mockMvc.perform(get("/api/v1/seasons/33/soil-tests")
                        .param("plotId", "22")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].id").value(31))
                .andExpect(jsonPath("$.result[0].status").value("measured"))
                .andExpect(jsonPath("$.result[0].sourceType").value("user_entered"));
    }

    @Test
    @DisplayName("POST soil-test validates required fields")
    void create_WhenValidationFails_ReturnsBadRequest() throws Exception {
        CreateSoilTestRequest invalidRequest = CreateSoilTestRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .build();

        mockMvc.perform(post("/api/v1/seasons/33/soil-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
