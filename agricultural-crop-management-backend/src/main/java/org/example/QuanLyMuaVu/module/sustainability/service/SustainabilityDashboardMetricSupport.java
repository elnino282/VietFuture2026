package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SustainabilityDashboardMetricSupport {

    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    public static final String METRIC_STATUS_MEASURED = "measured";
    public static final String METRIC_STATUS_ESTIMATED = "estimated";
    public static final String METRIC_STATUS_MISSING = "missing";
    public static final String METRIC_STATUS_UNAVAILABLE = "unavailable";

    Map<NutrientInputSource, BigDecimal> initInputMap() {
        Map<NutrientInputSource, BigDecimal> map = new LinkedHashMap<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            map.put(source, ZERO);
        }
        return map;
    }

    Map<NutrientInputSource, String> initMethodMap() {
        Map<NutrientInputSource, String> map = new LinkedHashMap<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            map.put(source, METRIC_STATUS_MISSING);
        }
        return map;
    }

    void mergeInputMap(Map<NutrientInputSource, BigDecimal> target, Map<NutrientInputSource, BigDecimal> incoming) {
        for (Map.Entry<NutrientInputSource, BigDecimal> entry : incoming.entrySet()) {
            target.put(entry.getKey(), safe(target.get(entry.getKey())).add(safe(entry.getValue())));
        }
    }

    void mergeMethods(Map<NutrientInputSource, String> target, Map<NutrientInputSource, String> incoming) {
        if (incoming == null) {
            return;
        }
        for (NutrientInputSource source : NutrientInputSource.values()) {
            String current = target.get(source);
            String next = incoming.get(source);
            target.put(source, mergeMethod(current, next));
        }
    }

    private String mergeMethod(String current, String next) {
        String c = normalize(current);
        String n = normalize(next);
        if (!StringUtils.hasText(c) || METRIC_STATUS_MISSING.equals(c)) {
            return StringUtils.hasText(n) ? n : METRIC_STATUS_MISSING;
        }
        if (!StringUtils.hasText(n) || METRIC_STATUS_MISSING.equals(n)) {
            return c;
        }
        return c.equals(n) ? c : "mixed";
    }

    List<SustainabilityOverviewResponse.DataInputQuality> buildDataQuality(Map<NutrientInputSource, String> methods) {
        if (methods == null || methods.isEmpty()) {
            return List.of();
        }
        List<SustainabilityOverviewResponse.DataInputQuality> out = new ArrayList<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            String method = StringUtils.hasText(methods.get(source)) ? methods.get(source) : METRIC_STATUS_MISSING;
            out.add(SustainabilityOverviewResponse.DataInputQuality.builder()
                    .source(source.name())
                    .method(method)
                    .confidence(confidenceByMethod(method))
                    .build());
        }
        return out;
    }

    private BigDecimal confidenceByMethod(String method) {
        return switch (normalize(method)) {
            case METRIC_STATUS_MEASURED -> BigDecimal.ONE;
            case "mixed" -> new BigDecimal("0.75");
            case METRIC_STATUS_ESTIMATED -> new BigDecimal("0.55");
            case METRIC_STATUS_MISSING -> new BigDecimal("0.20");
            default -> new BigDecimal("0.40");
        };
    }

    List<String> collectMissingInputs(Map<NutrientInputSource, String> methods) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<NutrientInputSource, String> entry : methods.entrySet()) {
            if (METRIC_STATUS_MISSING.equals(normalize(entry.getValue()))) {
                missing.add(entry.getKey().name());
            }
        }
        return missing;
    }

    SustainabilityOverviewResponse.MetricValue buildMetric(
            BigDecimal value,
            String unit,
            String status,
            BigDecimal confidence,
            String calculationMode,
            List<String> assumptions,
            List<String> missingInputs
    ) {
        return SustainabilityOverviewResponse.MetricValue.builder()
                .value(scale2(value))
                .unit(unit)
                .status(status)
                .confidence(scale2(confidence))
                .calculationMode(calculationMode)
                .assumptions(truncateList(assumptions, 5))
                .missingInputs(truncateList(missingInputs, 8))
                .build();
    }

    SustainabilityOverviewResponse.DataQualitySummary buildDataQualitySummary(
            List<SustainabilityOverviewResponse.DataInputQuality> dataQuality,
            BigDecimal overallConfidence
    ) {
        int measuredCount = 0;
        int estimatedCount = 0;
        int missingCount = 0;
        int unavailableCount = 0;
        for (SustainabilityOverviewResponse.DataInputQuality item : dataQuality) {
            String method = normalize(item.getMethod());
            switch (method) {
                case METRIC_STATUS_MEASURED -> measuredCount++;
                case METRIC_STATUS_ESTIMATED, "mixed" -> estimatedCount++;
                case METRIC_STATUS_MISSING -> missingCount++;
                default -> unavailableCount++;
            }
        }
        String summary = String.format(
                Locale.ROOT,
                "Overall confidence %s%%; %d measured, %d estimated/mixed, %d missing inputs.",
                scale2(safe(overallConfidence).multiply(HUNDRED)),
                measuredCount,
                estimatedCount,
                missingCount
        );
        return SustainabilityOverviewResponse.DataQualitySummary.builder()
                .overallConfidence(scale2(overallConfidence))
                .measuredInputCount(measuredCount)
                .estimatedInputCount(estimatedCount)
                .missingInputCount(missingCount)
                .unavailableInputCount(unavailableCount)
                .summary(summary)
                .build();
    }

    String resolveMetricStatusBySources(
            boolean hasContext,
            BigDecimal value,
            Map<NutrientInputSource, String> sourceMethods,
            List<NutrientInputSource> requiredSources
    ) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        if (value == null) {
            return METRIC_STATUS_MISSING;
        }
        if (sourceMethods == null || sourceMethods.isEmpty()) {
            return METRIC_STATUS_ESTIMATED;
        }
        boolean hasMeasured = false;
        boolean hasEstimatedOrMixed = false;
        boolean hasMissing = false;
        for (NutrientInputSource source : requiredSources) {
            String method = normalize(sourceMethods.get(source));
            switch (method) {
                case METRIC_STATUS_MEASURED -> hasMeasured = true;
                case METRIC_STATUS_ESTIMATED, "mixed" -> hasEstimatedOrMixed = true;
                case METRIC_STATUS_MISSING, "" -> hasMissing = true;
                default -> hasEstimatedOrMixed = true;
            }
        }
        if (hasMeasured && !hasEstimatedOrMixed && !hasMissing) {
            return METRIC_STATUS_MEASURED;
        }
        if (hasMeasured || hasEstimatedOrMixed) {
            return METRIC_STATUS_ESTIMATED;
        }
        return METRIC_STATUS_MISSING;
    }

    String resolveYieldStatus(boolean hasContext, Boolean yieldObserved) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        return Boolean.TRUE.equals(yieldObserved) ? METRIC_STATUS_MEASURED : METRIC_STATUS_MISSING;
    }

    String resolveOutputStatus(
            boolean hasContext,
            BigDecimal outputValue,
            Boolean yieldObserved,
            Boolean usedDefaultCropNContent
    ) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        if (outputValue == null || !Boolean.TRUE.equals(yieldObserved)) {
            return METRIC_STATUS_MISSING;
        }
        return Boolean.TRUE.equals(usedDefaultCropNContent) ? METRIC_STATUS_ESTIMATED : METRIC_STATUS_MEASURED;
    }

    String resolveNueStatus(
            boolean hasContext,
            BigDecimal nueValue,
            Boolean yieldObserved,
            Boolean usedDefaultCropNContent,
            Map<NutrientInputSource, String> sourceMethods
    ) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        if (nueValue == null || !Boolean.TRUE.equals(yieldObserved)) {
            return METRIC_STATUS_MISSING;
        }
        if (Boolean.TRUE.equals(usedDefaultCropNContent)) {
            return METRIC_STATUS_ESTIMATED;
        }
        if (sourceMethods == null || sourceMethods.isEmpty()) {
            return METRIC_STATUS_ESTIMATED;
        }
        boolean hasEstimated = false;
        boolean hasMeasured = false;
        for (String method : sourceMethods.values()) {
            String normalized = normalize(method);
            if (METRIC_STATUS_MEASURED.equals(normalized)) {
                hasMeasured = true;
            } else if (METRIC_STATUS_ESTIMATED.equals(normalized) || "mixed".equals(normalized) || METRIC_STATUS_MISSING.equals(normalized)) {
                hasEstimated = true;
            }
        }
        if (hasMeasured && !hasEstimated) {
            return METRIC_STATUS_MEASURED;
        }
        return METRIC_STATUS_ESTIMATED;
    }

    String resolveSurplusStatus(
            boolean hasContext,
            BigDecimal nSurplusValue,
            Boolean yieldObserved,
            Boolean usedDefaultCropNContent,
            Map<NutrientInputSource, String> sourceMethods
    ) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        if (nSurplusValue == null || !Boolean.TRUE.equals(yieldObserved)) {
            return METRIC_STATUS_MISSING;
        }
        if (Boolean.TRUE.equals(usedDefaultCropNContent)) {
            return METRIC_STATUS_ESTIMATED;
        }
        if (sourceMethods == null || sourceMethods.isEmpty()) {
            return METRIC_STATUS_ESTIMATED;
        }
        boolean hasEstimated = false;
        boolean hasMeasured = false;
        for (String method : sourceMethods.values()) {
            String normalized = normalize(method);
            if (METRIC_STATUS_MEASURED.equals(normalized)) {
                hasMeasured = true;
            } else if (METRIC_STATUS_ESTIMATED.equals(normalized) || "mixed".equals(normalized) || METRIC_STATUS_MISSING.equals(normalized)) {
                hasEstimated = true;
            }
        }
        if (hasMeasured && !hasEstimated) {
            return METRIC_STATUS_MEASURED;
        }
        return METRIC_STATUS_ESTIMATED;
    }

    String resolveScoreStatus(
            boolean hasContext,
            BigDecimal scoreValue,
            BigDecimal confidence,
            List<String> missingInputs
    ) {
        if (!hasContext) {
            return METRIC_STATUS_UNAVAILABLE;
        }
        if (scoreValue == null) {
            return METRIC_STATUS_MISSING;
        }
        if (safe(confidence).compareTo(new BigDecimal("0.60")) < 0 || (missingInputs != null && !missingInputs.isEmpty())) {
            return METRIC_STATUS_ESTIMATED;
        }
        return METRIC_STATUS_MEASURED;
    }

    Map<NutrientInputSource, BigDecimal> toRawInputMap(
            SustainabilityOverviewResponse.InputsBreakdown inputs,
            BigDecimal area
    ) {
        Map<NutrientInputSource, BigDecimal> out = initInputMap();
        if (inputs == null) {
            return out;
        }
        out.put(NutrientInputSource.MINERAL_FERTILIZER, toRaw(inputs.getMineralFertilizerN(), area));
        out.put(NutrientInputSource.ORGANIC_FERTILIZER, toRaw(inputs.getOrganicFertilizerN(), area));
        out.put(NutrientInputSource.BIOLOGICAL_FIXATION, toRaw(inputs.getBiologicalFixationN(), area));
        out.put(NutrientInputSource.IRRIGATION_WATER, toRaw(inputs.getIrrigationWaterN(), area));
        out.put(NutrientInputSource.ATMOSPHERIC_DEPOSITION, toRaw(inputs.getAtmosphericDepositionN(), area));
        out.put(NutrientInputSource.SEED_IMPORT, toRaw(inputs.getSeedImportN(), area));
        out.put(NutrientInputSource.SOIL_LEGACY, toRaw(inputs.getSoilLegacyN(), area));
        out.put(NutrientInputSource.CONTROL_SUPPLY, toRaw(inputs.getControlSupplyN(), area));
        return out;
    }

    SustainabilityOverviewResponse.InputsBreakdown buildBreakdown(
            Map<NutrientInputSource, BigDecimal> rawInputs,
            BigDecimal totalArea
    ) {
        return SustainabilityOverviewResponse.InputsBreakdown.builder()
                .mineralFertilizerN(scale2(toPerHa(rawInputs.get(NutrientInputSource.MINERAL_FERTILIZER), totalArea)))
                .organicFertilizerN(scale2(toPerHa(rawInputs.get(NutrientInputSource.ORGANIC_FERTILIZER), totalArea)))
                .biologicalFixationN(scale2(toPerHa(rawInputs.get(NutrientInputSource.BIOLOGICAL_FIXATION), totalArea)))
                .irrigationWaterN(scale2(toPerHa(rawInputs.get(NutrientInputSource.IRRIGATION_WATER), totalArea)))
                .atmosphericDepositionN(scale2(toPerHa(rawInputs.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION), totalArea)))
                .seedImportN(scale2(toPerHa(rawInputs.get(NutrientInputSource.SEED_IMPORT), totalArea)))
                .soilLegacyN(scale2(toPerHa(rawInputs.get(NutrientInputSource.SOIL_LEGACY), totalArea)))
                .controlSupplyN(scale2(toPerHa(rawInputs.get(NutrientInputSource.CONTROL_SUPPLY), totalArea)))
                .build();
    }

    Map<String, BigDecimal> aggregateComponents(List<SustainabilityDashboardService.FieldComputation> computations, BigDecimal totalArea) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        List<String> keys = List.of("dependency", "efficiency", "productivity", "risk", "confidence");
        for (String key : keys) {
            BigDecimal sum = ZERO;
            BigDecimal weightSum = ZERO;
            for (SustainabilityDashboardService.FieldComputation item : computations) {
                BigDecimal value = item.result().getScoreComponents() != null ? item.result().getScoreComponents().get(key) : null;
                if (value == null) {
                    continue;
                }
                BigDecimal weight = totalArea.compareTo(ZERO) > 0
                        ? Optional.ofNullable(positiveOrNull(item.areaHa())).orElse(BigDecimal.ONE)
                        : BigDecimal.ONE;
                sum = sum.add(value.multiply(weight));
                weightSum = weightSum.add(weight);
            }
            out.put(key, weightSum.compareTo(ZERO) > 0
                    ? scale2(sum.divide(weightSum, 2, RoundingMode.HALF_UP))
                    : BigDecimal.valueOf(50));
        }
        return out;
    }

    Map<String, BigDecimal> toWeightMap(AppProperties.ScoreWeights weights) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        out.put("dependency", safe(weights.getDependency()));
        out.put("efficiency", safe(weights.getEfficiency()));
        out.put("productivity", safe(weights.getProductivity()));
        out.put("risk", safe(weights.getRisk()));
        out.put("confidence", safe(weights.getConfidence()));
        return out;
    }

    BigDecimal computeWeightedScore(Map<String, BigDecimal> components, Map<String, BigDecimal> weights) {
        BigDecimal weighted = ZERO;
        BigDecimal weightSum = ZERO;
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            weighted = weighted.add(safe(components.get(entry.getKey())).multiply(safe(entry.getValue())));
            weightSum = weightSum.add(safe(entry.getValue()));
        }
        return weightSum.compareTo(ZERO) > 0 ? weighted.divide(weightSum, 2, RoundingMode.HALF_UP) : ZERO;
    }

    String mapScoreLabel(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.valueOf(50)) < 0) {
            return "Needs optimization";
        }
        if (score.compareTo(BigDecimal.valueOf(70)) < 0) {
            return "Fair";
        }
        if (score.compareTo(BigDecimal.valueOf(85)) < 0) {
            return "Good";
        }
        return "Excellent";
    }

    String resolveAlertLevel(BigDecimal fdnTotal, AppProperties.AlertThresholds thresholds) {
        if (fdnTotal == null) {
            return "medium";
        }
        if (fdnTotal.compareTo(safe(thresholds.getLowMaxExclusive())) < 0) {
            return "low";
        }
        if (fdnTotal.compareTo(safe(thresholds.getMediumMaxExclusive())) < 0) {
            return "medium";
        }
        return "high";
    }

    String resolveAlertExplanation(String level, AppProperties.AlertThresholds thresholds) {
        return switch (normalize(level)) {
            case "low" -> "FDN is below configured low threshold (" + thresholds.getLowMaxExclusive() + "%).";
            case "high" -> "FDN is above configured medium threshold (" + thresholds.getMediumMaxExclusive() + "%).";
            default -> "FDN is between configured low and high thresholds.";
        };
    }

    String resolveFarmMode(Set<String> modes) {
        if (modes.isEmpty()) {
            return "hybrid_estimated";
        }
        if (modes.size() == 1) {
            return modes.iterator().next();
        }
        if (modes.stream().allMatch("exact_control"::equals)) {
            return "exact_control";
        }
        if (modes.stream().allMatch("explicit_budget"::equals)) {
            return "explicit_budget";
        }
        return "hybrid_estimated";
    }

    List<String> withScienceNotes(List<String> notes, boolean farmScope) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (notes != null) {
            merged.addAll(notes);
        }
        merged.add("FDN and NUE are computed from scientific formulas in the project specification.");
        merged.add("Alert thresholds, recommendation rules, and sustainability score weights are configurable product rules.");
        if (farmScope) {
            merged.add("Farm scope aggregates field-level nitrogen inputs and outputs before computing FDN/NUE.");
        }
        return merged.stream().limit(20).toList();
    }

    String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.compareTo(ZERO) > 0 ? value : null;
    }

    BigDecimal toRaw(BigDecimal valuePerHa, BigDecimal area) {
        BigDecimal normalizedArea = positiveOrNull(area);
        return normalizedArea != null && valuePerHa != null ? valuePerHa.multiply(normalizedArea) : safe(valuePerHa);
    }

    BigDecimal toPerHa(BigDecimal valueKg, BigDecimal area) {
        BigDecimal normalizedArea = positiveOrNull(area);
        return normalizedArea != null && valueKg != null
                ? valueKg.divide(normalizedArea, 4, RoundingMode.HALF_UP)
                : safe(valueKg);
    }

    BigDecimal toRawYieldKg(SustainabilityCalculationService.CalculationResult result, BigDecimal area) {
        if (result == null || result.getYieldValue() == null) {
            return ZERO;
        }
        String unit = normalize(result.getYieldUnit());
        if ("t/ha".equals(unit) && positiveOrNull(area) != null) {
            return result.getYieldValue().multiply(BigDecimal.valueOf(1000)).multiply(area);
        }
        return result.getYieldValue();
    }

    private List<String> truncateList(List<String> values, int maxItems) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(StringUtils::hasText).limit(maxItems).toList();
    }
}
