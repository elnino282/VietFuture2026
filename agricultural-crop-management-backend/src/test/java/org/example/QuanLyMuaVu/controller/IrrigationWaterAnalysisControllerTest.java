package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.module.sustainability.controller.IrrigationWaterAnalysisController;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateIrrigationWaterAnalysisRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.IrrigationWaterAnalysisResponse;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.module.sustainability.service.IrrigationWaterAnalysisService;
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

@WebMvcTest(controllers = IrrigationWaterAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class IrrigationWaterAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IrrigationWaterAnalysisService irrigationWaterAnalysisService;

    @Test
    @DisplayName("POST irrigation-water-analysis returns stable ApiResponse contract")
    void create_ReturnsContractShape() throws Exception {
        CreateIrrigationWaterAnalysisRequest request = CreateIrrigationWaterAnalysisRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .nitrateMgPerL(new BigDecimal("4.2000"))
                .ammoniumMgPerL(new BigDecimal("1.8000"))
                .totalNmgPerL(null)
                .irrigationVolumeM3(new BigDecimal("500.0000"))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("water-lab-0318.pdf")
                .labReference("LAB-IRR-101")
                .note("Canal sample")
                .build();

        IrrigationWaterAnalysisResponse response = IrrigationWaterAnalysisResponse.builder()
                .id(101)
                .seasonId(33)
                .plotId(22)
                .plotName("Plot A")
                .sampleDate(LocalDate.of(2026, 3, 18))
                .nitrateMgPerL(new BigDecimal("4.2000"))
                .ammoniumMgPerL(new BigDecimal("1.8000"))
                .effectiveNmgPerL(new BigDecimal("6.0000"))
                .concentrationUnit("mg_n_per_l")
                .irrigationVolumeM3(new BigDecimal("500.0000"))
                .volumeUnit("m3")
                .estimatedNContributionKg(new BigDecimal("3.0000"))
                .contributionUnit("kg_n")
                .measured(true)
                .status("measured")
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("water-lab-0318.pdf")
                .labReference("LAB-IRR-101")
                .note("Canal sample")
                .createdByUserId(7L)
                .build();

        when(irrigationWaterAnalysisService.create(eq(33), any(CreateIrrigationWaterAnalysisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/seasons/33/irrigation-water-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(101))
                .andExpect(jsonPath("$.result.status").value("measured"))
                .andExpect(jsonPath("$.result.effectiveNmgPerL").value(6.0000))
                .andExpect(jsonPath("$.result.estimatedNContributionKg").value(3.0000))
                .andExpect(jsonPath("$.result.sourceType").value("lab_measured"));
    }

    @Test
    @DisplayName("GET irrigation-water-analyses returns list contract")
    void list_ReturnsListContractShape() throws Exception {
        when(irrigationWaterAnalysisService.list(33, 22))
                .thenReturn(List.of(
                        IrrigationWaterAnalysisResponse.builder()
                                .id(11)
                                .seasonId(33)
                                .plotId(22)
                                .sampleDate(LocalDate.of(2026, 3, 18))
                                .estimatedNContributionKg(new BigDecimal("3.0000"))
                                .status("measured")
                                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                                .build()
                ));

        mockMvc.perform(get("/api/v1/seasons/33/irrigation-water-analyses")
                        .param("plotId", "22")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].id").value(11))
                .andExpect(jsonPath("$.result[0].status").value("measured"))
                .andExpect(jsonPath("$.result[0].sourceType").value("lab_measured"));
    }

    @Test
    @DisplayName("POST irrigation-water-analysis validates required fields")
    void create_WhenValidationFails_ReturnsBadRequest() throws Exception {
        CreateIrrigationWaterAnalysisRequest invalidRequest = CreateIrrigationWaterAnalysisRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .build();

        mockMvc.perform(post("/api/v1/seasons/33/irrigation-water-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
