package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.module.sustainability.controller.NutrientInputController;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateNutrientInputRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.NutrientInputResponse;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.sustainability.service.NutrientInputIngestionService;
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

@WebMvcTest(controllers = NutrientInputController.class)
@AutoConfigureMockMvc(addFilters = false)
class NutrientInputControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NutrientInputIngestionService nutrientInputIngestionService;

    @Test
    @DisplayName("POST nutrient inputs returns ApiResponse with traceability fields")
    void createNutrientInput_ReturnsContractShape() throws Exception {
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(22)
                .inputSource(NutrientInputSource.MINERAL_FERTILIZER)
                .value(new BigDecimal("33.5"))
                .unit("kg_n")
                .recordedAt(LocalDate.of(2026, 3, 10))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("report-10-03-2026.pdf")
                .note("Measured in lab")
                .build();

        NutrientInputResponse response = NutrientInputResponse.builder()
                .id(101)
                .seasonId(33)
                .plotId(22)
                .plotName("Plot A")
                .inputSource(NutrientInputSource.MINERAL_FERTILIZER)
                .value(new BigDecimal("33.5000"))
                .unit("kg_n")
                .normalizedNKg(new BigDecimal("33.5000"))
                .recordedAt(LocalDate.of(2026, 3, 10))
                .measured(true)
                .status("measured")
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("report-10-03-2026.pdf")
                .note("Measured in lab")
                .createdByUserId(7L)
                .build();

        when(nutrientInputIngestionService.create(eq(33), any(CreateNutrientInputRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/seasons/33/nutrient-inputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result.id").value(101))
                .andExpect(jsonPath("$.result.status").value("measured"))
                .andExpect(jsonPath("$.result.value").value(33.5000))
                .andExpect(jsonPath("$.result.normalizedNKg").value(33.5000))
                .andExpect(jsonPath("$.result.sourceType").value("lab_measured"))
                .andExpect(jsonPath("$.result.sourceDocument").value("report-10-03-2026.pdf"));
    }

    @Test
    @DisplayName("POST nutrient inputs rejects deprecated legacy aggregate sources")
    void createNutrientInput_RejectsLegacySources() throws Exception {
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(22)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .value(new BigDecimal("9.2"))
                .unit("kg_n")
                .recordedAt(LocalDate.of(2026, 3, 12))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .note("Water sample")
                .build();

        when(nutrientInputIngestionService.create(eq(33), any(CreateNutrientInputRequest.class)))
                .thenThrow(new AppException(ErrorCode.LEGACY_NUTRIENT_INPUT_DEPRECATED));

        mockMvc.perform(post("/api/v1/seasons/33/nutrient-inputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_LEGACY_NUTRIENT_INPUT_DEPRECATED"));
    }

    @Test
    @DisplayName("GET nutrient inputs returns list with semantic measured/estimated status")
    void listNutrientInputs_ReturnsListContractShape() throws Exception {
        when(nutrientInputIngestionService.list(33, 22, NutrientInputSource.ORGANIC_FERTILIZER))
                .thenReturn(List.of(
                        NutrientInputResponse.builder()
                                .id(1)
                                .seasonId(33)
                                .plotId(22)
                                .inputSource(NutrientInputSource.ORGANIC_FERTILIZER)
                                .value(new BigDecimal("20.0000"))
                                .unit("kg_n")
                                .normalizedNKg(new BigDecimal("20.0000"))
                                .recordedAt(LocalDate.of(2026, 3, 11))
                                .status("measured")
                                .sourceType(NutrientInputSourceType.USER_ENTERED)
                                .build(),
                        NutrientInputResponse.builder()
                                .id(2)
                                .seasonId(33)
                                .plotId(22)
                                .inputSource(NutrientInputSource.ORGANIC_FERTILIZER)
                                .value(new BigDecimal("18.0000"))
                                .unit("kg_n")
                                .normalizedNKg(new BigDecimal("18.0000"))
                                .recordedAt(LocalDate.of(2026, 3, 9))
                                .status("estimated")
                                .sourceType(NutrientInputSourceType.EXTERNAL_REFERENCE)
                                .build()
                ));

        mockMvc.perform(get("/api/v1/seasons/33/nutrient-inputs")
                        .param("plotId", "22")
                        .param("source", "ORGANIC_FERTILIZER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].status").value("measured"))
                .andExpect(jsonPath("$.result[1].status").value("estimated"))
                .andExpect(jsonPath("$.result[1].sourceType").value("external_reference"));
    }
}
