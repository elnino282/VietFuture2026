package org.example.QuanLyMuaVu.module.sustainability.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldRecommendationsResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SustainabilityDashboardService {

    static final String RECOMMENDATION_SOURCE = "product_rule_config_v1";

    CurrentUserService currentUserService;
    FarmerOwnershipService ownershipService;
    FarmQueryPort farmQueryPort;
    SustainabilityCalculationService calculationService;
    SustainabilityRecommendationService recommendationService;
    AppProperties appProperties;
    SustainabilityDashboardContextService contextService;
    SustainabilityDashboardMetricSupport metricSupport;

    public SustainabilityOverviewResponse getOverview(String scope, Integer seasonId, Integer fieldId, Integer farmId) {
        return "farm".equals(metricSupport.normalize(scope))
                ? buildFarmOverview(seasonId, fieldId, farmId)
                : buildFieldOverview(seasonId, fieldId, farmId, true);
    }

    public SustainabilityOverviewResponse getFieldMetrics(Integer fieldId, Integer seasonId) {
        return buildFieldOverview(seasonId, fieldId, null, true);
    }

    public List<SustainabilityOverviewResponse.HistoryPoint> getFieldHistory(Integer fieldId) {
        return contextService.buildHistoryForPlot(ownershipService.requireOwnedPlot(fieldId));
    }

    public FieldRecommendationsResponse getFieldRecommendations(Integer fieldId, Integer seasonId) {
        SustainabilityDashboardContextService.FieldContext context = contextService.resolveFieldContext(seasonId, fieldId, null);
        SustainabilityCalculationService.CalculationResult result = context.season() != null
                ? calculationService.calculate(context.season(), context.plot())
                : SustainabilityCalculationService.CalculationResult.empty();
        return FieldRecommendationsResponse.builder()
                .fieldId(context.plot().getId())
                .seasonId(context.season() != null ? context.season().getId() : null)
                .fdnTotal(result.getFdnTotal())
                .fdnMineral(result.getFdnMineral())
                .nue(result.getNue())
                .confidence(result.getConfidence())
                .fdnLevel(result.getAlertLevel())
                .thresholdSource(result.getThresholdSource())
                .recommendationSource(RECOMMENDATION_SOURCE)
                .calculationMode(result.getCalculationMode())
                .missingInputs(result.getMissingInputs() != null ? result.getMissingInputs() : List.of())
                .recommendations(recommendationService.generate(result))
                .build();
    }

    public FieldMapResponse getFieldMap(Integer seasonId, Integer farmId, String cropName, String alertLevel) {
        Long ownerId = currentUserService.getCurrentUserId();
        org.example.QuanLyMuaVu.module.season.entity.Season selectedSeason = seasonId != null ? ownershipService.requireOwnedSeason(seasonId) : null;
        List<org.example.QuanLyMuaVu.module.farm.entity.Plot> plots;
        if (farmId != null) {
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm = ownershipService.requireOwnedFarm(farmId);
            plots = farmQueryPort.findPlotsByOwnerIdAndFarmId(ownerId, farm.getId());
        } else if (selectedSeason != null) {
            plots = List.of(selectedSeason.getPlot());
        } else {
            plots = farmQueryPort.findPlotsByOwnerId(ownerId);
        }

        String cropFilter = metricSupport.normalize(cropName);
        String levelFilter = metricSupport.normalize(alertLevel);
        List<FieldMapResponse.FieldMapItem> items = new ArrayList<>();
        for (org.example.QuanLyMuaVu.module.farm.entity.Plot plot : plots) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = contextService.resolveSeasonForMap(plot, selectedSeason);
            SustainabilityCalculationService.CalculationResult result = season != null
                    ? calculationService.calculate(season, plot)
                    : SustainabilityCalculationService.CalculationResult.empty();
            if (StringUtils.hasText(levelFilter) && !"all".equals(levelFilter)
                    && !levelFilter.equals(metricSupport.normalize(result.getAlertLevel()))) {
                continue;
            }
            if (StringUtils.hasText(cropFilter) && !"all".equals(cropFilter)) {
                String seasonCrop = season != null && season.getCrop() != null
                        ? metricSupport.normalize(season.getCrop().getCropName())
                        : "";
                if (!cropFilter.equals(seasonCrop)) {
                    continue;
                }
            }
            JsonNode geometry = contextService.parseGeometry(plot.getBoundaryGeoJson());
            items.add(FieldMapResponse.FieldMapItem.builder()
                    .fieldId(plot.getId())
                    .fieldName(plot.getPlotName())
                    .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                    .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                    .geometry(geometry)
                    .center(contextService.deriveCenter(geometry))
                    .cropName(season != null && season.getCrop() != null ? season.getCrop().getCropName() : "N/A")
                    .seasonName(season != null ? season.getSeasonName() : "No season")
                    .fdnLevel(result.getAlertLevel())
                    .fdnTotal(result.getFdnTotal())
                    .fdnMineral(result.getFdnMineral())
                    .fdnOrganic(result.getFdnOrganic())
                    .nue(result.getNue())
                    .confidence(result.getConfidence())
                    .calculationMode(result.getCalculationMode())
                    .thresholdSource(result.getThresholdSource())
                    .recommendationSource(RECOMMENDATION_SOURCE)
                    .missingInputs(result.getMissingInputs() != null ? result.getMissingInputs() : List.of())
                    .inputsBreakdown(result.getInputsBreakdown())
                    .recommendations(recommendationService.generate(result))
                    .build());
        }
        return FieldMapResponse.builder().items(items).build();
    }

    private SustainabilityOverviewResponse buildFieldOverview(Integer seasonId, Integer fieldId, Integer farmId, boolean includeHistory) {
        SustainabilityDashboardContextService.FieldContext context = contextService.resolveFieldContext(seasonId, fieldId, farmId);
        SustainabilityCalculationService.CalculationResult result = context.season() != null
                ? calculationService.calculate(context.season(), context.plot())
                : SustainabilityCalculationService.CalculationResult.empty();
        List<String> notes = metricSupport.withScienceNotes(result.getNotes(), false);
        List<SustainabilityOverviewResponse.DataInputQuality> dataQuality = metricSupport.buildDataQuality(result.getSourceMethod());
        List<String> missingInputs = result.getMissingInputs() != null ? result.getMissingInputs() : List.of();
        AppProperties.AlertThresholds thresholds = resolveAlertThresholds();
        boolean hasContext = context.season() != null;

        String scoreStatus = metricSupport.resolveScoreStatus(hasContext, result.getSustainabilityScore(), result.getConfidence(), missingInputs);
        String fdnTotalStatus = metricSupport.resolveMetricStatusBySources(hasContext, result.getFdnTotal(), result.getSourceMethod(), List.of(NutrientInputSource.MINERAL_FERTILIZER, NutrientInputSource.ORGANIC_FERTILIZER));
        String fdnMineralStatus = metricSupport.resolveMetricStatusBySources(hasContext, result.getFdnMineral(), result.getSourceMethod(), List.of(NutrientInputSource.MINERAL_FERTILIZER));
        String fdnOrganicStatus = metricSupport.resolveMetricStatusBySources(hasContext, result.getFdnOrganic(), result.getSourceMethod(), List.of(NutrientInputSource.ORGANIC_FERTILIZER));
        String yieldStatus = metricSupport.resolveYieldStatus(hasContext, result.getYieldObserved());
        String nOutputStatus = metricSupport.resolveOutputStatus(hasContext, result.getNOutput(), result.getYieldObserved(), result.getUsedDefaultCropNContent());
        String nueStatus = metricSupport.resolveNueStatus(hasContext, result.getNue(), result.getYieldObserved(), result.getUsedDefaultCropNContent(), result.getSourceMethod());
        String nSurplusStatus = metricSupport.resolveSurplusStatus(hasContext, result.getNSurplus(), result.getYieldObserved(), result.getUsedDefaultCropNContent(), result.getSourceMethod());

        return SustainabilityOverviewResponse.builder()
                .scope("field")
                .entityId(String.valueOf(context.plot().getId()))
                .seasonId(context.season() != null ? context.season().getId() : null)
                .calculationMode(result.getCalculationMode())
                .confidence(result.getConfidence())
                .sustainableScore(SustainabilityOverviewResponse.SustainabilityScore.builder()
                        .value(result.getSustainabilityScore())
                        .label(result.getSustainabilityLabel())
                        .components(result.getScoreComponents())
                        .weights(result.getScoreWeights())
                        .build())
                .fdn(SustainabilityOverviewResponse.FdnMetrics.builder()
                        .total(result.getFdnTotal())
                        .mineral(result.getFdnMineral())
                        .organic(result.getFdnOrganic())
                        .level(result.getAlertLevel())
                        .status(fdnTotalStatus)
                        .thresholdSource(result.getThresholdSource())
                        .lowMaxExclusive(metricSupport.scale2(thresholds.getLowMaxExclusive()))
                        .mediumMaxExclusive(metricSupport.scale2(thresholds.getMediumMaxExclusive()))
                        .mineralHighMin(metricSupport.scale2(thresholds.getMineralHighMin()))
                        .explanation(result.getAlertExplanation())
                        .build())
                .nue(result.getNue())
                .nOutput(result.getNOutput())
                .nSurplus(result.getNSurplus())
                .currentSeason(contextService.buildCurrentSeason(context.season()))
                .yield(SustainabilityOverviewResponse.YieldSummary.builder().estimated(result.getYieldValue()).unit(result.getYieldUnit()).build())
                .inputsBreakdown(result.getInputsBreakdown())
                .unit(result.getUnit())
                .dataQuality(dataQuality)
                .dataQualitySummary(metricSupport.buildDataQualitySummary(dataQuality, result.getConfidence()))
                .missingInputs(missingInputs)
                .notes(notes)
                .recommendations(recommendationService.generate(result))
                .recommendationSource(RECOMMENDATION_SOURCE)
                .sustainableScoreMetric(metricSupport.buildMetric(result.getSustainabilityScore(), "%", scoreStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .fdnTotalMetric(metricSupport.buildMetric(result.getFdnTotal(), "%", fdnTotalStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .fdnMineralMetric(metricSupport.buildMetric(result.getFdnMineral(), "%", fdnMineralStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .fdnOrganicMetric(metricSupport.buildMetric(result.getFdnOrganic(), "%", fdnOrganicStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .nueMetric(metricSupport.buildMetric(result.getNue(), "%", nueStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .nOutputMetric(metricSupport.buildMetric(result.getNOutput(), result.getUnit(), nOutputStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .nSurplusMetric(metricSupport.buildMetric(result.getNSurplus(), result.getUnit(), nSurplusStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .estimatedYieldMetric(metricSupport.buildMetric(result.getYieldValue(), result.getYieldUnit(), yieldStatus, result.getConfidence(), result.getCalculationMode(), notes, missingInputs))
                .historicalTrend(includeHistory ? contextService.buildHistoryForPlot(context.plot()) : List.of())
                .build();
    }

    private SustainabilityOverviewResponse buildFarmOverview(Integer seasonId, Integer fieldId, Integer farmId) {
        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = contextService.resolveFarmContext(seasonId, fieldId, farmId);
        if (farm == null) {
            return buildEmptyOverview("farm");
        }

        List<FieldComputation> computations = collectFarmComputations(farm, seasonId);
        if (computations.isEmpty()) {
            SustainabilityOverviewResponse empty = buildEmptyOverview("farm");
            empty.setEntityId(String.valueOf(farm.getId()));
            empty.setNotes(metricSupport.withScienceNotes(List.of("No season data available for this farm context."), true));
            return empty;
        }

        Map<NutrientInputSource, BigDecimal> rawInputs = metricSupport.initInputMap();
        Map<NutrientInputSource, String> mergedMethods = metricSupport.initMethodMap();
        BigDecimal totalArea = SustainabilityDashboardMetricSupport.ZERO;
        BigDecimal totalInputsRaw = SustainabilityDashboardMetricSupport.ZERO;
        BigDecimal totalOutputRaw = SustainabilityDashboardMetricSupport.ZERO;
        BigDecimal totalYieldRawKg = SustainabilityDashboardMetricSupport.ZERO;
        BigDecimal confidenceWeighted = SustainabilityDashboardMetricSupport.ZERO;
        BigDecimal confidenceWeight = SustainabilityDashboardMetricSupport.ZERO;
        Set<String> modes = new LinkedHashSet<>();
        List<String> allNotes = new ArrayList<>();

        for (FieldComputation computation : computations) {
            metricSupport.mergeInputMap(rawInputs, computation.rawInputs());
            metricSupport.mergeMethods(mergedMethods, computation.result().getSourceMethod());
            totalInputsRaw = totalInputsRaw.add(computation.totalInputsRaw());
            totalOutputRaw = totalOutputRaw.add(computation.nOutputRaw());
            totalYieldRawKg = totalYieldRawKg.add(computation.rawYieldKg());
            modes.add(metricSupport.normalize(computation.result().getCalculationMode()));
            allNotes.addAll(computation.result().getNotes());

            BigDecimal area = metricSupport.positiveOrNull(computation.areaHa());
            BigDecimal weight = area != null ? area : BigDecimal.ONE;
            if (area != null) {
                totalArea = totalArea.add(area);
            }
            confidenceWeighted = confidenceWeighted.add(metricSupport.safe(computation.result().getConfidence()).multiply(weight));
            confidenceWeight = confidenceWeight.add(weight);
        }

        BigDecimal mineralRaw = rawInputs.get(NutrientInputSource.MINERAL_FERTILIZER);
        BigDecimal organicRaw = rawInputs.get(NutrientInputSource.ORGANIC_FERTILIZER);
        BigDecimal fertilizerRaw = metricSupport.safe(mineralRaw).add(metricSupport.safe(organicRaw));
        BigDecimal fdnTotal = metricSupport.percent(fertilizerRaw, totalInputsRaw);
        BigDecimal fdnMineral = metricSupport.percent(mineralRaw, totalInputsRaw);
        BigDecimal fdnOrganic = metricSupport.percent(organicRaw, totalInputsRaw);
        BigDecimal nSurplusRaw = totalInputsRaw.subtract(totalOutputRaw);
        BigDecimal confidence = confidenceWeight.compareTo(SustainabilityDashboardMetricSupport.ZERO) > 0
                ? confidenceWeighted.divide(confidenceWeight, 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(0.10);

        AppProperties.Sustainability cfg = resolveSustainabilityConfig();
        AppProperties.AlertThresholds thresholds = cfg.getAlerts() != null ? cfg.getAlerts() : new AppProperties.AlertThresholds();
        AppProperties.ScoreWeights weights = cfg.getScoreWeights() != null ? cfg.getScoreWeights() : new AppProperties.ScoreWeights();

        String level = metricSupport.resolveAlertLevel(fdnTotal, thresholds);
        String mode = metricSupport.resolveFarmMode(modes);
        Map<String, BigDecimal> components = metricSupport.aggregateComponents(computations, totalArea);
        Map<String, BigDecimal> scoreWeights = metricSupport.toWeightMap(weights);
        BigDecimal score = metricSupport.computeWeightedScore(components, scoreWeights);
        boolean hasYieldObserved = totalYieldRawKg.compareTo(SustainabilityDashboardMetricSupport.ZERO) > 0;
        BigDecimal nue = hasYieldObserved ? metricSupport.percent(totalOutputRaw, totalInputsRaw) : null;

        SustainabilityOverviewResponse.InputsBreakdown breakdown = metricSupport.buildBreakdown(rawInputs, totalArea);
        BigDecimal nOutput = hasYieldObserved ? metricSupport.scale2(metricSupport.toPerHa(totalOutputRaw, totalArea)) : null;
        BigDecimal nSurplus = hasYieldObserved ? metricSupport.scale2(metricSupport.toPerHa(nSurplusRaw, totalArea)) : null;
        List<String> notes = metricSupport.withScienceNotes(allNotes, true);
        List<String> recommendations = recommendationService.generate(fdnTotal, fdnMineral, nue, nSurplus, confidence, breakdown, mergedMethods);
        List<SustainabilityOverviewResponse.DataInputQuality> dataQuality = metricSupport.buildDataQuality(mergedMethods);
        List<String> missingInputs = metricSupport.collectMissingInputs(mergedMethods);

        String scoreStatus = metricSupport.resolveScoreStatus(true, score, confidence, missingInputs);
        String fdnTotalStatus = metricSupport.resolveMetricStatusBySources(true, metricSupport.scale2(fdnTotal), mergedMethods, List.of(NutrientInputSource.MINERAL_FERTILIZER, NutrientInputSource.ORGANIC_FERTILIZER));
        String fdnMineralStatus = metricSupport.resolveMetricStatusBySources(true, metricSupport.scale2(fdnMineral), mergedMethods, List.of(NutrientInputSource.MINERAL_FERTILIZER));
        String fdnOrganicStatus = metricSupport.resolveMetricStatusBySources(true, metricSupport.scale2(fdnOrganic), mergedMethods, List.of(NutrientInputSource.ORGANIC_FERTILIZER));
        String yieldStatus = hasYieldObserved ? SustainabilityDashboardMetricSupport.METRIC_STATUS_MEASURED : SustainabilityDashboardMetricSupport.METRIC_STATUS_MISSING;
        String nOutputStatus = metricSupport.resolveOutputStatus(true, nOutput, hasYieldObserved, false);
        String nueStatus = metricSupport.resolveNueStatus(true, metricSupport.scale2(nue), hasYieldObserved, false, mergedMethods);
        String nSurplusStatus = metricSupport.resolveSurplusStatus(true, nSurplus, hasYieldObserved, false, mergedMethods);

        SustainabilityOverviewResponse.YieldSummary yieldSummary;
        if (totalArea.compareTo(SustainabilityDashboardMetricSupport.ZERO) > 0) {
            BigDecimal value = hasYieldObserved
                    ? totalYieldRawKg.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP).divide(totalArea, 2, RoundingMode.HALF_UP)
                    : null;
            yieldSummary = SustainabilityOverviewResponse.YieldSummary.builder().estimated(value).unit("t/ha").build();
        } else {
            yieldSummary = SustainabilityOverviewResponse.YieldSummary.builder()
                    .estimated(hasYieldObserved ? totalYieldRawKg.setScale(2, RoundingMode.HALF_UP) : null)
                    .unit("kg")
                    .build();
        }

        org.example.QuanLyMuaVu.module.season.entity.Season latestSeason = computations.stream()
                .map(FieldComputation::season)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(org.example.QuanLyMuaVu.module.season.entity.Season::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        String outputUnit = totalArea.compareTo(SustainabilityDashboardMetricSupport.ZERO) > 0
                ? "kg N/ha/season"
                : "kg N/season (farm area missing)";

        return SustainabilityOverviewResponse.builder()
                .scope("farm")
                .entityId(String.valueOf(farm.getId()))
                .seasonId(seasonId)
                .calculationMode(mode)
                .confidence(confidence)
                .sustainableScore(SustainabilityOverviewResponse.SustainabilityScore.builder()
                        .value(metricSupport.scale2(score))
                        .label(metricSupport.mapScoreLabel(score))
                        .components(components)
                        .weights(scoreWeights)
                        .build())
                .fdn(SustainabilityOverviewResponse.FdnMetrics.builder()
                        .total(metricSupport.scale2(fdnTotal))
                        .mineral(metricSupport.scale2(fdnMineral))
                        .organic(metricSupport.scale2(fdnOrganic))
                        .level(level)
                        .status(fdnTotalStatus)
                        .thresholdSource(cfg.getThresholdSource())
                        .lowMaxExclusive(metricSupport.scale2(thresholds.getLowMaxExclusive()))
                        .mediumMaxExclusive(metricSupport.scale2(thresholds.getMediumMaxExclusive()))
                        .mineralHighMin(metricSupport.scale2(thresholds.getMineralHighMin()))
                        .explanation(metricSupport.resolveAlertExplanation(level, thresholds))
                        .build())
                .nue(metricSupport.scale2(nue))
                .nOutput(nOutput)
                .nSurplus(nSurplus)
                .currentSeason(contextService.buildCurrentSeason(latestSeason))
                .yield(yieldSummary)
                .inputsBreakdown(breakdown)
                .unit(outputUnit)
                .dataQuality(dataQuality)
                .dataQualitySummary(metricSupport.buildDataQualitySummary(dataQuality, confidence))
                .missingInputs(missingInputs)
                .notes(notes)
                .recommendations(recommendations)
                .recommendationSource(RECOMMENDATION_SOURCE)
                .sustainableScoreMetric(metricSupport.buildMetric(metricSupport.scale2(score), "%", scoreStatus, confidence, mode, notes, missingInputs))
                .fdnTotalMetric(metricSupport.buildMetric(metricSupport.scale2(fdnTotal), "%", fdnTotalStatus, confidence, mode, notes, missingInputs))
                .fdnMineralMetric(metricSupport.buildMetric(metricSupport.scale2(fdnMineral), "%", fdnMineralStatus, confidence, mode, notes, missingInputs))
                .fdnOrganicMetric(metricSupport.buildMetric(metricSupport.scale2(fdnOrganic), "%", fdnOrganicStatus, confidence, mode, notes, missingInputs))
                .nueMetric(metricSupport.buildMetric(metricSupport.scale2(nue), "%", nueStatus, confidence, mode, notes, missingInputs))
                .nOutputMetric(metricSupport.buildMetric(nOutput, outputUnit, nOutputStatus, confidence, mode, notes, missingInputs))
                .nSurplusMetric(metricSupport.buildMetric(nSurplus, outputUnit, nSurplusStatus, confidence, mode, notes, missingInputs))
                .estimatedYieldMetric(metricSupport.buildMetric(yieldSummary.getEstimated(), yieldSummary.getUnit(), yieldStatus, confidence, mode, notes, missingInputs))
                .historicalTrend(List.of())
                .build();
    }

    private SustainabilityOverviewResponse buildEmptyOverview(String scope) {
        SustainabilityCalculationService.CalculationResult empty = SustainabilityCalculationService.CalculationResult.empty();
        AppProperties.AlertThresholds thresholds = resolveAlertThresholds();
        List<String> notes = metricSupport.withScienceNotes(empty.getNotes(), "farm".equals(scope));
        List<SustainabilityOverviewResponse.DataInputQuality> dataQuality = metricSupport.buildDataQuality(empty.getSourceMethod());
        List<String> missingInputs = empty.getMissingInputs() != null ? empty.getMissingInputs() : List.of();

        return SustainabilityOverviewResponse.builder()
                .scope(scope)
                .entityId(null)
                .seasonId(null)
                .calculationMode(empty.getCalculationMode())
                .confidence(empty.getConfidence())
                .sustainableScore(SustainabilityOverviewResponse.SustainabilityScore.builder()
                        .value(empty.getSustainabilityScore())
                        .label(empty.getSustainabilityLabel())
                        .components(empty.getScoreComponents())
                        .weights(empty.getScoreWeights())
                        .build())
                .fdn(SustainabilityOverviewResponse.FdnMetrics.builder()
                        .total(empty.getFdnTotal())
                        .mineral(empty.getFdnMineral())
                        .organic(empty.getFdnOrganic())
                        .level(empty.getAlertLevel())
                        .status(SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE)
                        .thresholdSource(empty.getThresholdSource())
                        .lowMaxExclusive(metricSupport.scale2(thresholds.getLowMaxExclusive()))
                        .mediumMaxExclusive(metricSupport.scale2(thresholds.getMediumMaxExclusive()))
                        .mineralHighMin(metricSupport.scale2(thresholds.getMineralHighMin()))
                        .explanation(empty.getAlertExplanation())
                        .build())
                .nue(empty.getNue())
                .nOutput(empty.getNOutput())
                .nSurplus(empty.getNSurplus())
                .yield(SustainabilityOverviewResponse.YieldSummary.builder().estimated(empty.getYieldValue()).unit(empty.getYieldUnit()).build())
                .inputsBreakdown(empty.getInputsBreakdown())
                .unit(empty.getUnit())
                .dataQuality(dataQuality)
                .dataQualitySummary(metricSupport.buildDataQualitySummary(dataQuality, empty.getConfidence()))
                .missingInputs(missingInputs)
                .notes(notes)
                .recommendations(recommendationService.generate(empty))
                .recommendationSource(RECOMMENDATION_SOURCE)
                .sustainableScoreMetric(metricSupport.buildMetric(null, "%", SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .fdnTotalMetric(metricSupport.buildMetric(null, "%", SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .fdnMineralMetric(metricSupport.buildMetric(null, "%", SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .fdnOrganicMetric(metricSupport.buildMetric(null, "%", SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .nueMetric(metricSupport.buildMetric(null, "%", SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .nOutputMetric(metricSupport.buildMetric(null, empty.getUnit(), SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .nSurplusMetric(metricSupport.buildMetric(null, empty.getUnit(), SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .estimatedYieldMetric(metricSupport.buildMetric(null, empty.getYieldUnit(), SustainabilityDashboardMetricSupport.METRIC_STATUS_UNAVAILABLE, empty.getConfidence(), empty.getCalculationMode(), notes, missingInputs))
                .historicalTrend(List.of())
                .build();
    }

    private List<FieldComputation> collectFarmComputations(org.example.QuanLyMuaVu.module.farm.entity.Farm farm, Integer seasonId) {
        List<FieldComputation> out = new ArrayList<>();
        if (seasonId != null) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
            if (season.getPlot() == null || season.getPlot().getFarm() == null
                    || !Objects.equals(season.getPlot().getFarm().getId(), farm.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            out.add(toComputation(season.getPlot(), season));
            return out;
        }
        Long ownerId = currentUserService.getCurrentUserId();
        for (org.example.QuanLyMuaVu.module.farm.entity.Plot plot : farmQueryPort.findPlotsByOwnerIdAndFarmId(ownerId, farm.getId())) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = contextService.resolvePreferredSeasonForPlot(plot.getId());
            if (season != null) {
                out.add(toComputation(plot, season));
            }
        }
        return out;
    }

    private FieldComputation toComputation(org.example.QuanLyMuaVu.module.farm.entity.Plot plot, org.example.QuanLyMuaVu.module.season.entity.Season season) {
        SustainabilityCalculationService.CalculationResult result = calculationService.calculate(season, plot);
        BigDecimal area = metricSupport.positiveOrNull(plot.getArea());
        Map<NutrientInputSource, BigDecimal> rawInputs = metricSupport.toRawInputMap(result.getInputsBreakdown(), area);
        return new FieldComputation(
                plot,
                season,
                result,
                area,
                rawInputs,
                rawInputs.values().stream().reduce(SustainabilityDashboardMetricSupport.ZERO, BigDecimal::add),
                metricSupport.toRaw(result.getNOutput(), area),
                metricSupport.toRawYieldKg(result, area)
        );
    }

    private AppProperties.Sustainability resolveSustainabilityConfig() {
        return appProperties.getSustainability() != null
                ? appProperties.getSustainability()
                : new AppProperties.Sustainability();
    }

    private AppProperties.AlertThresholds resolveAlertThresholds() {
        AppProperties.Sustainability cfg = resolveSustainabilityConfig();
        return cfg.getAlerts() != null ? cfg.getAlerts() : new AppProperties.AlertThresholds();
    }

    record FieldComputation(
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            SustainabilityCalculationService.CalculationResult result,
            BigDecimal areaHa,
            Map<NutrientInputSource, BigDecimal> rawInputs,
            BigDecimal totalInputsRaw,
            BigDecimal nOutputRaw,
            BigDecimal rawYieldKg
    ) {
    }
}
