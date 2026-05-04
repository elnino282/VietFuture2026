package org.example.QuanLyMuaVu.Service.Sustainability;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.incident.entity.Alert;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.CropNitrogenReference;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SustainabilityCalculationServiceTest {

    @Mock
    private NutrientInputEventRepository nutrientInputEventRepository;
    @Mock
    private ExpenseQueryPort expenseQueryPort;
    @Mock
    private HarvestQueryPort harvestQueryPort;
    @Mock
    private CropCatalogQueryPort cropCatalogQueryPort;
    @Mock
    private SeasonQueryPort seasonQueryPort;
    @Mock
    private IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    @Mock
    private SoilTestRepository soilTestRepository;

    private AppProperties appProperties;
    private SustainabilityCalculationService service;
    private Season season;
    private Plot plot;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new SustainabilityCalculationService(
                nutrientInputEventRepository,
                expenseQueryPort,
                harvestQueryPort,
                cropCatalogQueryPort,
                seasonQueryPort,
                irrigationWaterAnalysisRepository,
                soilTestRepository,
                appProperties
        );

        Crop crop = Crop.builder()
                .id(11)
                .cropName("Rice")
                .build();

        plot = Plot.builder()
                .id(22)
                .plotName("Plot A")
                .area(new BigDecimal("1.00"))
                .build();

        season = Season.builder()
                .id(33)
                .seasonName("Season A")
                .plot(plot)
                .crop(crop)
                .startDate(LocalDate.now().minusDays(20))
                .build();
    }

    @Test
    @DisplayName("Measured inputs + observed yield => computed FDN/NUE/N output with high confidence")
    void calculate_WhenMeasuredInputsAndYieldPresent_ReturnsMeasuredLikeResult() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "60", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "5", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.SOIL_LEGACY, "4", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("2000"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertNotNull(result.getFdnTotal());
        assertNotNull(result.getNue());
        assertNotNull(result.getNOutput());
        assertNotNull(result.getNSurplus());
        assertNotNull(result.getYieldValue());
        assertEquals("explicit_budget", result.getCalculationMode());
        assertEquals(new BigDecimal("1.00"), result.getConfidence());
        assertEquals("measured", result.getSourceMethod().get(NutrientInputSource.MINERAL_FERTILIZER));
    }

    @Test
    @DisplayName("No observed yield => output-dependent metrics are null (not fake zero)")
    void calculate_WhenYieldMissing_OutputMetricsAreNull() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "40", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(BigDecimal.ZERO);

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertNull(result.getNOutput());
        assertNull(result.getNue());
        assertNull(result.getNSurplus());
        assertNull(result.getYieldValue());
    }

    @Test
    @DisplayName("True zero value is preserved when denominator exists")
    void calculate_WhenMineralIsTrueZero_PreservesZeroInsteadOfMissing() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "0", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "40", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "30", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "20", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "10", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1000"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertNotNull(result.getFdnMineral());
        assertEquals(0, result.getFdnMineral().compareTo(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("Changing threshold config changes alert level deterministically")
    void calculate_WhenThresholdChanges_AlertLevelChanges() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "40", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "20", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "10", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "10", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1500"));

        SustainabilityCalculationService.CalculationResult baseline = service.calculate(season, plot);
        assertEquals("medium", baseline.getAlertLevel());

        appProperties.getSustainability().getAlerts().setMediumMaxExclusive(new BigDecimal("50"));
        SustainabilityCalculationService.CalculationResult tightened = service.calculate(season, plot);
        assertEquals("high", tightened.getAlertLevel());
    }

    @Test
    @DisplayName("Input sourceType traceability can upgrade method to measured when measured flag is absent/false")
    void calculate_WhenSourceTypeIsMeasured_ShouldMarkSourceAsMeasured() {
        stubCommonCalculatedDependencies();
        NutrientInputEvent labEvent = event(NutrientInputSource.MINERAL_FERTILIZER, "50", false);
        labEvent.setSourceType(NutrientInputSourceType.LAB_MEASURED);

        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        labEvent,
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "10", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "5", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.SOIL_LEGACY, "3", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1200"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertEquals("measured", result.getSourceMethod().get(NutrientInputSource.MINERAL_FERTILIZER));
        assertEquals(new BigDecimal("1.00"), result.getConfidence());
    }

    @Test
    @DisplayName("Soil test ingestion changes SOIL_LEGACY from missing to measured and improves confidence")
    void calculate_WhenSoilLegacyMeasured_ShouldImproveConfidenceAndClearMissingInput() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "45", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "15", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "5", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true)
                ))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "45", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "15", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "5", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.SOIL_LEGACY, "8", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1800"));

        SustainabilityCalculationService.CalculationResult missingSoilResult = service.calculate(season, plot);
        SustainabilityCalculationService.CalculationResult measuredSoilResult = service.calculate(season, plot);

        assertTrue(missingSoilResult.getMissingInputs().contains(NutrientInputSource.SOIL_LEGACY.name()));
        assertNull(missingSoilResult.getInputsBreakdown().getSoilLegacyN());
        assertEquals(new BigDecimal("0.92"), missingSoilResult.getConfidence());

        assertFalse(measuredSoilResult.getMissingInputs().contains(NutrientInputSource.SOIL_LEGACY.name()));
        assertEquals(new BigDecimal("8.00"), measuredSoilResult.getInputsBreakdown().getSoilLegacyN());
        assertEquals(new BigDecimal("1.00"), measuredSoilResult.getConfidence());
    }

    @Test
    @DisplayName("Measured irrigation input overrides IRRIGATE-log estimation and increases confidence")
    void calculate_WhenIrrigationMeasured_ShouldOverrideFallbackEstimate() {
        stubCommonCalculatedDependencies();
        when(seasonQueryPort.countFieldLogsBySeasonAndLogType(season.getId(), "IRRIGATE"))
                .thenReturn(2L);
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "40", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "12", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "6", true),
                        event(NutrientInputSource.SOIL_LEGACY, "8", true)
                ))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "40", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "12", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "6", true),
                        event(NutrientInputSource.SOIL_LEGACY, "8", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "9", true)
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1700"));

        SustainabilityCalculationService.CalculationResult estimatedIrrigationResult = service.calculate(season, plot);
        SustainabilityCalculationService.CalculationResult measuredIrrigationResult = service.calculate(season, plot);

        assertEquals("estimated", estimatedIrrigationResult.getSourceMethod().get(NutrientInputSource.IRRIGATION_WATER));
        assertEquals(new BigDecimal("2.40"), estimatedIrrigationResult.getInputsBreakdown().getIrrigationWaterN());
        assertEquals(new BigDecimal("0.88"), estimatedIrrigationResult.getConfidence());

        assertEquals("measured", measuredIrrigationResult.getSourceMethod().get(NutrientInputSource.IRRIGATION_WATER));
        assertEquals(new BigDecimal("9.00"), measuredIrrigationResult.getInputsBreakdown().getIrrigationWaterN());
        assertEquals(new BigDecimal("1.00"), measuredIrrigationResult.getConfidence());
    }

    @Test
    @DisplayName("Dedicated irrigation-water-analysis should override aggregate irrigation input")
    void calculate_WhenDedicatedIrrigationAnalysisExists_ShouldOverrideAggregateIrrigationSource() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "40", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "20", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "2", false),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.SOIL_LEGACY, "6", true)
                ));
        when(irrigationWaterAnalysisRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        IrrigationWaterAnalysis.builder()
                                .season(season)
                                .plot(plot)
                                .sampleDate(LocalDate.of(2026, 3, 21))
                                .totalNmgPerL(new BigDecimal("10"))
                                .irrigationVolumeM3(new BigDecimal("500"))
                                .measured(true)
                                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                                .build()
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1200"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertEquals("measured", result.getSourceMethod().get(NutrientInputSource.IRRIGATION_WATER));
        assertEquals(new BigDecimal("5.00"), result.getInputsBreakdown().getIrrigationWaterN());
    }

    @Test
    @DisplayName("Dedicated soil-test should override aggregate soil legacy contribution")
    void calculate_WhenDedicatedSoilTestExists_ShouldOverrideAggregateSoilLegacy() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "45", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "15", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "6", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.SOIL_LEGACY, "2", false)
                ));
        when(soilTestRepository.findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        SoilTest.builder()
                                .season(season)
                                .plot(plot)
                                .sampleDate(LocalDate.of(2026, 3, 19))
                                .mineralNKgPerHa(new BigDecimal("8"))
                                .measured(true)
                                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                                .build()
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1600"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertEquals("measured", result.getSourceMethod().get(NutrientInputSource.SOIL_LEGACY));
        assertEquals(new BigDecimal("8.00"), result.getInputsBreakdown().getSoilLegacyN());
    }

    @Test
    @DisplayName("Migrated dedicated records should override legacy aggregate values without double count")
    void calculate_WhenMigratedDedicatedRecordsExist_ShouldAvoidDoubleCount() {
        stubCommonCalculatedDependencies();
        when(nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        event(NutrientInputSource.MINERAL_FERTILIZER, "45", true),
                        event(NutrientInputSource.ORGANIC_FERTILIZER, "15", true),
                        event(NutrientInputSource.BIOLOGICAL_FIXATION, "10", true),
                        event(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "5", true),
                        event(NutrientInputSource.IRRIGATION_WATER, "7", true),
                        event(NutrientInputSource.SOIL_LEGACY, "9", true)
                ));
        when(irrigationWaterAnalysisRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        IrrigationWaterAnalysis.builder()
                                .season(season)
                                .plot(plot)
                                .sampleDate(LocalDate.of(2026, 3, 22))
                                .legacyNContributionKg(new BigDecimal("5"))
                                .legacyDerived(true)
                                .legacyEventId(501)
                                .measured(false)
                                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                                .build()
                ));
        when(soilTestRepository.findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(season.getId(), plot.getId()))
                .thenReturn(List.of(
                        SoilTest.builder()
                                .season(season)
                                .plot(plot)
                                .sampleDate(LocalDate.of(2026, 3, 22))
                                .mineralNKgPerHa(BigDecimal.ZERO)
                                .legacyNContributionKg(new BigDecimal("4"))
                                .legacyDerived(true)
                                .legacyEventId(601)
                                .measured(false)
                                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                                .build()
                ));
        when(harvestQueryPort.sumQuantityBySeasonId(season.getId())).thenReturn(new BigDecimal("1400"));

        SustainabilityCalculationService.CalculationResult result = service.calculate(season, plot);

        assertEquals(new BigDecimal("5.00"), result.getInputsBreakdown().getIrrigationWaterN());
        assertEquals(new BigDecimal("4.00"), result.getInputsBreakdown().getSoilLegacyN());
        assertEquals("estimated", result.getSourceMethod().get(NutrientInputSource.IRRIGATION_WATER));
        assertEquals("estimated", result.getSourceMethod().get(NutrientInputSource.SOIL_LEGACY));
    }

    @Test
    @DisplayName("Empty context returns null metrics and null breakdown values")
    void empty_WhenNoContext_ReturnsNullMetrics() {
        SustainabilityCalculationService.CalculationResult empty = SustainabilityCalculationService.CalculationResult.empty();

        assertNull(empty.getFdnTotal());
        assertNull(empty.getFdnMineral());
        assertNull(empty.getNue());
        assertNull(empty.getNOutput());
        assertNull(empty.getNSurplus());
        assertNull(empty.getYieldValue());
        SustainabilityOverviewResponse.InputsBreakdown breakdown = empty.getInputsBreakdown();
        assertNotNull(breakdown);
        assertNull(breakdown.getMineralFertilizerN());
        assertNull(breakdown.getOrganicFertilizerN());
        assertTrue(empty.getMissingInputs().contains(NutrientInputSource.MINERAL_FERTILIZER.name()));
    }

    private NutrientInputEvent event(NutrientInputSource source, String value, boolean measured) {
        return NutrientInputEvent.builder()
                .season(season)
                .plot(plot)
                .inputSource(source)
                .nKg(new BigDecimal(value))
                .measured(measured)
                .build();
    }

    private void stubCommonCalculatedDependencies() {
        lenient().when(expenseQueryPort.findFertilizerExpensesBySeasonId(season.getId()))
                .thenReturn(List.of());
        lenient().when(seasonQueryPort.countFieldLogsBySeasonAndLogType(season.getId(), "IRRIGATE")).thenReturn(0L);
        lenient().when(irrigationWaterAnalysisRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId()))
                .thenReturn(List.of());
        lenient().when(soilTestRepository.findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(season.getId(), plot.getId()))
                .thenReturn(List.of());
        when(cropCatalogQueryPort.findActiveNitrogenReferenceByCropId(season.getCrop().getId()))
                .thenReturn(Optional.of(CropNitrogenReference.builder()
                        .crop(season.getCrop())
                        .nContentKgPerKgYield(new BigDecimal("0.0100"))
                        .active(true)
                        .build()));
    }
}

