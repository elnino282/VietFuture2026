package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.controller.SustainabilityController;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldRecommendationsResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardService;
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
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SustainabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
class SustainabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SustainabilityDashboardService sustainabilityDashboardService;

    @Test
    @DisplayName("GET overview returns ApiResponse contract with metric wrappers and semantic fields")
    void getOverview_ReturnsSemanticContractShape() throws Exception {
        when(sustainabilityDashboardService.getOverview(eq("field"), eq(1), isNull(), isNull()))
                .thenReturn(sampleOverview());

        mockMvc.perform(get("/api/v1/dashboard/sustainability/overview")
                        .param("scope", "field")
                        .param("seasonId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result.fdn.status").value("estimated"))
                .andExpect(jsonPath("$.result.fdn.thresholdSource").value("config_default_v1"))
                .andExpect(jsonPath("$.result.fdn.lowMaxExclusive").value(40.00))
                .andExpect(jsonPath("$.result.fdn.mediumMaxExclusive").value(70.00))
                .andExpect(jsonPath("$.result.fdnTotalMetric.status").value("estimated"))
                .andExpect(jsonPath("$.result.fdnTotalMetric.assumptions").isArray())
                .andExpect(jsonPath("$.result.nSurplusMetric.status").value("estimated"))
                .andExpect(jsonPath("$.result.recommendationSource").value("product_rule_config_v1"));
    }

    @Test
    @DisplayName("GET overview keeps missing numeric fields as null instead of coercing to 0")
    void getOverview_DoesNotCoerceNullToZero() throws Exception {
        SustainabilityOverviewResponse response = sampleOverview();
        response.setNOutput(null);
        response.setNSurplus(null);
        response.setNOutputMetric(SustainabilityOverviewResponse.MetricValue.builder()
                .value(null)
                .unit("kg N/ha/season")
                .status("missing")
                .confidence(new BigDecimal("0.62"))
                .calculationMode("hybrid_estimated")
                .assumptions(List.of("Yield data missing"))
                .missingInputs(List.of("HARVEST_OUTPUT"))
                .build());
        response.setNSurplusMetric(SustainabilityOverviewResponse.MetricValue.builder()
                .value(null)
                .unit("kg N/ha/season")
                .status("missing")
                .confidence(new BigDecimal("0.62"))
                .calculationMode("hybrid_estimated")
                .assumptions(List.of("Yield data missing"))
                .missingInputs(List.of("HARVEST_OUTPUT"))
                .build());

        when(sustainabilityDashboardService.getOverview(eq("field"), eq(2), isNull(), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/sustainability/overview")
                        .param("scope", "field")
                        .param("seasonId", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nOutput").value(nullValue()))
                .andExpect(jsonPath("$.result.nSurplus").value(nullValue()))
                .andExpect(jsonPath("$.result.nOutputMetric.value").value(nullValue()))
                .andExpect(jsonPath("$.result.nSurplusMetric.value").value(nullValue()));
    }

    @Test
    @DisplayName("GET field metrics uses the same semantic wrapper shape as overview")
    void getFieldMetrics_ReturnsSemanticWrapperContract() throws Exception {
        when(sustainabilityDashboardService.getFieldMetrics(22, 33))
                .thenReturn(sampleOverview());

        mockMvc.perform(get("/api/v1/fields/22/sustainability-metrics")
                        .param("seasonId", "33")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.fdnTotalMetric.status").value("estimated"))
                .andExpect(jsonPath("$.result.fdnTotalMetric.calculationMode").value("hybrid_estimated"))
                .andExpect(jsonPath("$.result.fdnTotalMetric.missingInputs").isArray())
                .andExpect(jsonPath("$.result.nOutputMetric.status").value("estimated"));
    }

    @Test
    @DisplayName("GET field history returns stable key names including nOutput")
    void getFieldHistory_ReturnsStableHistoryContract() throws Exception {
        when(sustainabilityDashboardService.getFieldHistory(22))
                .thenReturn(List.of(
                        SustainabilityOverviewResponse.HistoryPoint.builder()
                                .seasonId(30)
                                .seasonName("Season 30")
                                .startDate(LocalDate.of(2025, 1, 10))
                                .fdnTotal(new BigDecimal("61.20"))
                                .fdnMineral(new BigDecimal("48.50"))
                                .fdnOrganic(new BigDecimal("12.70"))
                                .nue(new BigDecimal("52.40"))
                                .nOutput(new BigDecimal("39.10"))
                                .yield(new BigDecimal("6.00"))
                                .build()
                ));

        mockMvc.perform(get("/api/v1/fields/22/fdn-history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result[0].seasonId").value(30))
                .andExpect(jsonPath("$.result[0].nOutput").value(39.10))
                .andExpect(jsonPath("$.result[0].fdnTotal").value(61.20));
    }

    @Test
    @DisplayName("GET field recommendations exposes source-of-truth metadata")
    void getFieldRecommendations_ReturnsSourceMetadata() throws Exception {
        when(sustainabilityDashboardService.getFieldRecommendations(22, 33))
                .thenReturn(FieldRecommendationsResponse.builder()
                        .fieldId(22)
                        .seasonId(33)
                        .fdnTotal(new BigDecimal("72.00"))
                        .fdnMineral(new BigDecimal("60.00"))
                        .nue(new BigDecimal("43.00"))
                        .confidence(new BigDecimal("0.62"))
                        .fdnLevel("high")
                        .thresholdSource("config_default_v1")
                        .recommendationSource("product_rule_config_v1")
                        .calculationMode("hybrid_estimated")
                        .missingInputs(List.of("IRRIGATION_WATER"))
                        .recommendations(List.of("Measure irrigation-water nitrogen before changing mineral inputs."))
                        .build());

        mockMvc.perform(get("/api/v1/fields/22/recommendations")
                        .param("seasonId", "33")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.fdnLevel").value("high"))
                .andExpect(jsonPath("$.result.thresholdSource").value("config_default_v1"))
                .andExpect(jsonPath("$.result.recommendationSource").value("product_rule_config_v1"))
                .andExpect(jsonPath("$.result.missingInputs[0]").value("IRRIGATION_WATER"));
    }

    @Test
    @DisplayName("GET field map keeps null metric values and includes metadata fields")
    void getFieldMap_ReturnsStableShapeWithSemanticNulls() throws Exception {
        when(sustainabilityDashboardService.getFieldMap(33, null, null, "all"))
                .thenReturn(FieldMapResponse.builder()
                        .items(List.of(
                                FieldMapResponse.FieldMapItem.builder()
                                        .fieldId(22)
                                        .fieldName("Field A")
                                        .farmId(7)
                                        .farmName("Farm 7")
                                        .geometry(null)
                                        .center(null)
                                        .cropName("Rice")
                                        .seasonName("Season 33")
                                        .fdnLevel("high")
                                        .fdnTotal(null)
                                        .fdnMineral(null)
                                        .fdnOrganic(null)
                                        .nue(null)
                                        .confidence(new BigDecimal("0.55"))
                                        .calculationMode("hybrid_estimated")
                                        .thresholdSource("config_default_v1")
                                        .recommendationSource("product_rule_config_v1")
                                        .missingInputs(List.of("MINERAL_FERTILIZER"))
                                        .inputsBreakdown(SustainabilityOverviewResponse.InputsBreakdown.builder()
                                                .mineralFertilizerN(null)
                                                .organicFertilizerN(null)
                                                .biologicalFixationN(null)
                                                .irrigationWaterN(null)
                                                .atmosphericDepositionN(null)
                                                .seedImportN(null)
                                                .soilLegacyN(null)
                                                .controlSupplyN(null)
                                                .build())
                                        .recommendations(List.of("Collect measured fertilizer records first."))
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/api/v1/fields/map")
                        .param("seasonId", "33")
                        .param("alertLevel", "all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.items[0].fdnTotal").value(nullValue()))
                .andExpect(jsonPath("$.result.items[0].fdnMineral").value(nullValue()))
                .andExpect(jsonPath("$.result.items[0].calculationMode").value("hybrid_estimated"))
                .andExpect(jsonPath("$.result.items[0].thresholdSource").value("config_default_v1"))
                .andExpect(jsonPath("$.result.items[0].recommendationSource").value("product_rule_config_v1"));
    }

    private SustainabilityOverviewResponse sampleOverview() {
        SustainabilityOverviewResponse.MetricValue measuredMetric = SustainabilityOverviewResponse.MetricValue.builder()
                .value(new BigDecimal("72.00"))
                .unit("%")
                .status("estimated")
                .confidence(new BigDecimal("0.62"))
                .calculationMode("hybrid_estimated")
                .assumptions(List.of("Used estimated atmospheric deposition"))
                .missingInputs(List.of("CONTROL_SUPPLY"))
                .build();

        return SustainabilityOverviewResponse.builder()
                .scope("field")
                .entityId("22")
                .seasonId(1)
                .calculationMode("hybrid_estimated")
                .confidence(new BigDecimal("0.62"))
                .sustainableScore(SustainabilityOverviewResponse.SustainabilityScore.builder()
                        .value(new BigDecimal("64.00"))
                        .label("Fair")
                        .components(Map.of("dependency", new BigDecimal("28.00")))
                        .weights(Map.of("dependency", new BigDecimal("0.30")))
                        .build())
                .fdn(SustainabilityOverviewResponse.FdnMetrics.builder()
                        .total(new BigDecimal("72.00"))
                        .mineral(new BigDecimal("60.00"))
                        .organic(new BigDecimal("12.00"))
                        .level("high")
                        .status("estimated")
                        .thresholdSource("config_default_v1")
                        .lowMaxExclusive(new BigDecimal("40.00"))
                        .mediumMaxExclusive(new BigDecimal("70.00"))
                        .mineralHighMin(new BigDecimal("60.00"))
                        .explanation("FDN is above configured medium threshold (70%).")
                        .build())
                .nue(new BigDecimal("43.00"))
                .nOutput(new BigDecimal("41.00"))
                .nSurplus(new BigDecimal("36.00"))
                .yield(SustainabilityOverviewResponse.YieldSummary.builder()
                        .estimated(new BigDecimal("6.20"))
                        .unit("t/ha")
                        .build())
                .inputsBreakdown(SustainabilityOverviewResponse.InputsBreakdown.builder()
                        .mineralFertilizerN(new BigDecimal("60.00"))
                        .organicFertilizerN(new BigDecimal("12.00"))
                        .biologicalFixationN(new BigDecimal("8.00"))
                        .irrigationWaterN(new BigDecimal("6.00"))
                        .atmosphericDepositionN(new BigDecimal("5.00"))
                        .seedImportN(new BigDecimal("0.00"))
                        .soilLegacyN(new BigDecimal("0.00"))
                        .controlSupplyN(null)
                        .build())
                .unit("kg N/ha/season")
                .dataQuality(List.of(
                        SustainabilityOverviewResponse.DataInputQuality.builder()
                                .source("MINERAL_FERTILIZER")
                                .method("measured")
                                .confidence(BigDecimal.ONE)
                                .build()
                ))
                .dataQualitySummary(SustainabilityOverviewResponse.DataQualitySummary.builder()
                        .overallConfidence(new BigDecimal("0.62"))
                        .measuredInputCount(1)
                        .estimatedInputCount(3)
                        .missingInputCount(1)
                        .unavailableInputCount(0)
                        .summary("Overall confidence 62%; 1 measured, 3 estimated/mixed, 1 missing inputs.")
                        .build())
                .missingInputs(List.of("CONTROL_SUPPLY"))
                .notes(List.of("FDN and NUE are computed from scientific formulas in the project specification."))
                .recommendations(List.of("Measure irrigation-water nitrogen before changing mineral inputs."))
                .recommendationSource("product_rule_config_v1")
                .sustainableScoreMetric(measuredMetric)
                .fdnTotalMetric(measuredMetric)
                .fdnMineralMetric(measuredMetric)
                .fdnOrganicMetric(measuredMetric)
                .nueMetric(measuredMetric)
                .nOutputMetric(SustainabilityOverviewResponse.MetricValue.builder()
                        .value(new BigDecimal("41.00"))
                        .unit("kg N/ha/season")
                        .status("estimated")
                        .confidence(new BigDecimal("0.62"))
                        .calculationMode("hybrid_estimated")
                        .assumptions(List.of("Used estimated atmospheric deposition"))
                        .missingInputs(List.of("CONTROL_SUPPLY"))
                        .build())
                .nSurplusMetric(SustainabilityOverviewResponse.MetricValue.builder()
                        .value(new BigDecimal("36.00"))
                        .unit("kg N/ha/season")
                        .status("estimated")
                        .confidence(new BigDecimal("0.62"))
                        .calculationMode("hybrid_estimated")
                        .assumptions(List.of("Used estimated atmospheric deposition"))
                        .missingInputs(List.of("CONTROL_SUPPLY"))
                        .build())
                .estimatedYieldMetric(SustainabilityOverviewResponse.MetricValue.builder()
                        .value(new BigDecimal("6.20"))
                        .unit("t/ha")
                        .status("estimated")
                        .confidence(new BigDecimal("0.62"))
                        .calculationMode("hybrid_estimated")
                        .assumptions(List.of("Used estimated atmospheric deposition"))
                        .missingInputs(List.of("CONTROL_SUPPLY"))
                        .build())
                .historicalTrend(List.of())
                .build();
    }
}
