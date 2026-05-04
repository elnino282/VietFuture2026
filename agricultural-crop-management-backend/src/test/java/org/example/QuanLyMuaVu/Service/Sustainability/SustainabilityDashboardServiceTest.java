package org.example.QuanLyMuaVu.Service.Sustainability;

import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldRecommendationsResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityCalculationService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardContextService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardMetricSupport;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityRecommendationService;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SustainabilityDashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private FarmerOwnershipService ownershipService;
    @Mock
    private FarmQueryPort farmQueryPort;
    @Mock
    private SeasonQueryPort seasonQueryPort;
    @Mock
    private SustainabilityCalculationService calculationService;
    @Mock
    private SustainabilityRecommendationService recommendationService;

    private AppProperties appProperties;
    private SustainabilityDashboardService service;
    private Plot plot;
    private Season season;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        SustainabilityDashboardMetricSupport metricSupport = new SustainabilityDashboardMetricSupport();
        SustainabilityDashboardContextService contextService = new SustainabilityDashboardContextService(
                currentUserService,
                ownershipService,
                farmQueryPort,
                seasonQueryPort,
                calculationService,
                metricSupport,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        service = new SustainabilityDashboardService(
                currentUserService,
                ownershipService,
                farmQueryPort,
                calculationService,
                recommendationService,
                appProperties,
                contextService,
                metricSupport
        );

        Farm farm = Farm.builder()
                .id(7)
                .name("Farm 7")
                .build();
        Crop crop = Crop.builder()
                .id(9)
                .cropName("Rice")
                .build();
        plot = Plot.builder()
                .id(22)
                .plotName("Field A")
                .farm(farm)
                .area(new BigDecimal("1.50"))
                .build();
        season = Season.builder()
                .id(33)
                .seasonName("Season 33")
                .plot(plot)
                .crop(crop)
                .status(SeasonStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(25))
                .build();
    }

    @Test
    @DisplayName("Overview field contract includes metric semantics + thresholds from backend")
    void getOverview_WhenFieldContextPresent_ReturnsSemanticContract() {
        SustainabilityCalculationService.CalculationResult result = sampleCalculationResult();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(calculationService.calculate(eq(season), eq(plot))).thenReturn(result);
        when(seasonQueryPort.findAllSeasonsByPlotIdOrderByStartDateAsc(plot.getId())).thenReturn(List.of(season));
        when(recommendationService.generate(result)).thenReturn(List.of("Measure irrigation-water nitrogen before changing mineral inputs."));

        SustainabilityOverviewResponse response = service.getOverview("field", 33, null, null);

        assertNotNull(response);
        assertEquals("field", response.getScope());
        assertEquals("estimated", response.getFdn().getStatus());
        assertEquals(new BigDecimal("40.00"), response.getFdn().getLowMaxExclusive());
        assertEquals(new BigDecimal("70.00"), response.getFdn().getMediumMaxExclusive());
        assertEquals("product_rule_config_v1", response.getRecommendationSource());
        assertNotNull(response.getNSurplusMetric());
        assertEquals("estimated", response.getNSurplusMetric().getStatus());
        assertEquals(1, response.getHistoricalTrend().size());
    }

    @Test
    @DisplayName("Overview without season context returns unavailable/missing semantics and null values")
    void getOverview_WhenNoSeasonContext_DoesNotFakeZeroMetrics() {
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(seasonQueryPort.findAllSeasonsByPlotIdOrderByStartDateDesc(plot.getId())).thenReturn(List.of());
        when(recommendationService.generate(any(SustainabilityCalculationService.CalculationResult.class)))
                .thenReturn(List.of("Collect complete nutrient input and harvest data before applying strong optimization actions."));

        SustainabilityOverviewResponse response = service.getOverview("field", null, 22, null);

        assertNotNull(response);
        assertNull(response.getFdn().getTotal());
        assertEquals("unavailable", response.getFdn().getStatus());
        assertNull(response.getNOutput());
        assertNull(response.getNSurplus());
        assertEquals("unavailable", response.getNOutputMetric().getStatus());
        assertEquals("unavailable", response.getNSurplusMetric().getStatus());
    }

    @Test
    @DisplayName("Field map for plots without season keeps metric values null instead of 0")
    void getFieldMap_WhenSeasonMissing_UsesNullMetricValues() {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(farmQueryPort.findPlotsByOwnerId(1L)).thenReturn(List.of(plot));
        when(seasonQueryPort.findAllSeasonsByPlotIdOrderByStartDateDesc(plot.getId())).thenReturn(List.of());
        when(recommendationService.generate(any(SustainabilityCalculationService.CalculationResult.class)))
                .thenReturn(List.of("Collect complete nutrient input and harvest data before applying strong optimization actions."));

        FieldMapResponse map = service.getFieldMap(null, null, null, null);

        assertNotNull(map);
        assertEquals(1, map.getItems().size());
        FieldMapResponse.FieldMapItem item = map.getItems().get(0);
        assertNull(item.getFdnTotal());
        assertNull(item.getFdnMineral());
        assertNull(item.getFdnOrganic());
        assertNull(item.getNue());
        assertEquals("config_default_v1", item.getThresholdSource());
        assertEquals("product_rule_config_v1", item.getRecommendationSource());
    }

    @Test
    @DisplayName("Field recommendations expose backend recommendation + threshold source metadata")
    void getFieldRecommendations_ShouldExposeSourceMetadata() {
        SustainabilityCalculationService.CalculationResult result = sampleCalculationResult();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(calculationService.calculate(eq(season), eq(plot))).thenReturn(result);
        when(recommendationService.generate(result))
                .thenReturn(List.of("Measure irrigation-water nitrogen before changing mineral inputs."));

        FieldRecommendationsResponse response = service.getFieldRecommendations(plot.getId(), 33);

        assertNotNull(response);
        assertEquals(plot.getId(), response.getFieldId());
        assertEquals("high", response.getFdnLevel());
        assertEquals("config_default_v1", response.getThresholdSource());
        assertEquals("product_rule_config_v1", response.getRecommendationSource());
        assertEquals("hybrid_estimated", response.getCalculationMode());
        assertEquals(List.of("CONTROL_SUPPLY"), response.getMissingInputs());
    }

    private SustainabilityCalculationService.CalculationResult sampleCalculationResult() {
        Map<NutrientInputSource, String> sourceMethod = new LinkedHashMap<>();
        sourceMethod.put(NutrientInputSource.MINERAL_FERTILIZER, "measured");
        sourceMethod.put(NutrientInputSource.ORGANIC_FERTILIZER, "estimated");
        sourceMethod.put(NutrientInputSource.BIOLOGICAL_FIXATION, "estimated");
        sourceMethod.put(NutrientInputSource.IRRIGATION_WATER, "estimated");
        sourceMethod.put(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "estimated");
        sourceMethod.put(NutrientInputSource.SEED_IMPORT, "estimated");
        sourceMethod.put(NutrientInputSource.SOIL_LEGACY, "estimated");
        sourceMethod.put(NutrientInputSource.CONTROL_SUPPLY, "missing");

        SustainabilityOverviewResponse.InputsBreakdown breakdown = SustainabilityOverviewResponse.InputsBreakdown.builder()
                .mineralFertilizerN(new BigDecimal("60.00"))
                .organicFertilizerN(new BigDecimal("12.00"))
                .biologicalFixationN(new BigDecimal("8.00"))
                .irrigationWaterN(new BigDecimal("6.00"))
                .atmosphericDepositionN(new BigDecimal("5.00"))
                .seedImportN(new BigDecimal("0.00"))
                .soilLegacyN(new BigDecimal("0.00"))
                .controlSupplyN(null)
                .build();

        Map<String, BigDecimal> components = new LinkedHashMap<>();
        components.put("dependency", new BigDecimal("28.00"));
        components.put("efficiency", new BigDecimal("62.00"));
        components.put("productivity", new BigDecimal("70.00"));
        components.put("risk", new BigDecimal("58.00"));
        components.put("confidence", new BigDecimal("66.00"));

        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        weights.put("dependency", new BigDecimal("0.30"));
        weights.put("efficiency", new BigDecimal("0.25"));
        weights.put("productivity", new BigDecimal("0.20"));
        weights.put("risk", new BigDecimal("0.15"));
        weights.put("confidence", new BigDecimal("0.10"));

        return SustainabilityCalculationService.CalculationResult.builder()
                .calculationMode("hybrid_estimated")
                .confidence(new BigDecimal("0.66"))
                .fdnTotal(new BigDecimal("72.00"))
                .fdnMineral(new BigDecimal("60.00"))
                .fdnOrganic(new BigDecimal("12.00"))
                .alertLevel("high")
                .alertExplanation("FDN is above configured medium threshold (70%).")
                .nue(new BigDecimal("44.00"))
                .nOutput(new BigDecimal("42.00"))
                .nSurplus(new BigDecimal("38.00"))
                .inputsBreakdown(breakdown)
                .sourceMethod(sourceMethod)
                .missingInputs(List.of("CONTROL_SUPPLY"))
                .notes(List.of("Sample note"))
                .sustainabilityScore(new BigDecimal("63.00"))
                .sustainabilityLabel("Fair")
                .scoreComponents(components)
                .scoreWeights(weights)
                .thresholdSource("config_default_v1")
                .yieldValue(new BigDecimal("6.10"))
                .yieldUnit("t/ha")
                .unit("kg N/ha/season")
                .yieldObserved(true)
                .usedDefaultCropNContent(false)
                .build();
    }
}

