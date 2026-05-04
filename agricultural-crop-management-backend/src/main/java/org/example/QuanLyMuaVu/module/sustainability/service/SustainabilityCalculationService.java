package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.module.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SustainabilityCalculationService {

    NutrientInputEventRepository nutrientInputEventRepository;
    ExpenseQueryPort expenseQueryPort;
    HarvestQueryPort harvestQueryPort;
    CropCatalogQueryPort cropCatalogQueryPort;
    SeasonQueryPort seasonQueryPort;
    IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    SoilTestRepository soilTestRepository;
    AppProperties appProperties;

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final BigDecimal ZERO = BigDecimal.ZERO;
    static final Set<String> ORGANIC_KEYWORDS = Set.of(
            "organic", "manure", "compost", "humic", "bio", "vi sinh", "huu co"
    );
    static final Set<String> LEGUME_KEYWORDS = Set.of(
            "bean", "soy", "pea", "peanut", "clover", "legume", "dau", "lac"
    );

    public CalculationResult calculateForSeason(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null || season.getPlot() == null) {
            return CalculationResult.empty();
        }
        return calculate(season, season.getPlot());
    }

    public CalculationResult calculate(org.example.QuanLyMuaVu.module.season.entity.Season season, org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        if (season == null || plot == null) {
            return CalculationResult.empty();
        }

        AppProperties.Sustainability cfg = appProperties.getSustainability() != null
                ? appProperties.getSustainability()
                : new AppProperties.Sustainability();
        AppProperties.EstimationDefaults est = cfg.getEstimation() != null
                ? cfg.getEstimation()
                : new AppProperties.EstimationDefaults();
        AppProperties.AlertThresholds thresholds = cfg.getAlerts() != null
                ? cfg.getAlerts()
                : new AppProperties.AlertThresholds();
        AppProperties.ScoreWeights weights = cfg.getScoreWeights() != null
                ? cfg.getScoreWeights()
                : new AppProperties.ScoreWeights();

        BigDecimal areaHa = positiveOrNull(plot.getArea());
        List<String> notes = new ArrayList<>();

        Map<NutrientInputSource, BigDecimal> inputKg = initInputMap();
        Map<NutrientInputSource, String> sourceMethod = initSourceMethodMap();

        applyExplicitInputEvents(season, plot, inputKg, sourceMethod);
        applyDedicatedIrrigationWaterAnalyses(season, plot, inputKg, sourceMethod, notes);
        applyDedicatedSoilTests(season, plot, inputKg, sourceMethod, notes);
        estimateFertilizerFromExpenses(season, inputKg, sourceMethod, est, notes);
        estimateBiologicalFixation(season, areaHa, inputKg, sourceMethod, est, notes);
        estimateIrrigationWater(season, areaHa, inputKg, sourceMethod, est, notes);
        estimateAtmosphericDeposition(areaHa, inputKg, sourceMethod, est, notes);
        estimateOptionalSources(inputKg, sourceMethod, notes);

        String mode = resolveCalculationMode(inputKg, sourceMethod);
        BigDecimal totalInputsKg = sumInputsByMode(inputKg, mode);

        BigDecimal harvestFromBatchesKg = Optional
                .ofNullable(harvestQueryPort.sumQuantityBySeasonId(season.getId()))
                .orElse(ZERO);
        boolean hasHarvestBatches = harvestFromBatchesKg.compareTo(ZERO) > 0;
        boolean hasActualYieldRecord = season.getActualYieldKg() != null;

        BigDecimal harvestQuantityKg = harvestFromBatchesKg;
        if (isZero(harvestQuantityKg) && hasActualYieldRecord) {
            harvestQuantityKg = season.getActualYieldKg();
            notes.add("Used season.actualYieldKg as harvest output because harvest batches are empty.");
        }
        boolean yieldObserved = hasHarvestBatches || hasActualYieldRecord;

        BigDecimal nContent = resolveCropNContent(season, est, notes);
        boolean usedDefaultCropNContent = notes.stream()
                .anyMatch(note -> note.contains("default crop N concentration"));
        BigDecimal nOutputKg = yieldObserved ? harvestQuantityKg.multiply(nContent) : null;

        BigDecimal fdnTotal = percent(
                inputKg.get(NutrientInputSource.MINERAL_FERTILIZER)
                        .add(inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER)),
                totalInputsKg
        );
        BigDecimal fdnMineral = percent(inputKg.get(NutrientInputSource.MINERAL_FERTILIZER), totalInputsKg);
        BigDecimal fdnOrganic = percent(inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER), totalInputsKg);
        BigDecimal nue = percent(nOutputKg, totalInputsKg);
        BigDecimal nSurplusKg = nOutputKg != null ? totalInputsKg.subtract(nOutputKg) : null;

        String level = resolveAlertLevel(fdnTotal, thresholds);
        String alertExplanation = resolveAlertExplanation(level, thresholds);
        List<String> missingInputs = collectMissingInputs(sourceMethod);
        BigDecimal confidence = calculateConfidence(sourceMethod, areaHa != null, nContent, est);

        Map<String, BigDecimal> componentScores = buildComponentScores(
                fdnTotal,
                nue,
                nSurplusKg,
                season,
                harvestQuantityKg,
                confidence
        );
        BigDecimal sustainabilityScore = computeScore(componentScores, weights);
        String sustainabilityLabel = mapScoreLabel(sustainabilityScore);

        String unit = areaHa != null ? "kg N/ha/season" : "kg N/season (plot area missing)";
        SustainabilityOverviewResponse.InputsBreakdown breakdown = buildBreakdown(inputKg, sourceMethod, areaHa);
        BigDecimal nOutput = toPerUnit(nOutputKg, areaHa);
        BigDecimal nSurplus = toPerUnit(nSurplusKg, areaHa);

        BigDecimal yieldForCard = null;
        String yieldUnit;
        if (areaHa != null) {
            if (yieldObserved) {
                yieldForCard = harvestQuantityKg
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                        .divide(areaHa, 2, RoundingMode.HALF_UP);
            }
            yieldUnit = "t/ha";
        } else {
            if (yieldObserved) {
                yieldForCard = harvestQuantityKg.setScale(2, RoundingMode.HALF_UP);
            }
            yieldUnit = "kg";
        }

        return CalculationResult.builder()
                .calculationMode(mode)
                .confidence(confidence)
                .fdnTotal(scale2(fdnTotal))
                .fdnMineral(scale2(fdnMineral))
                .fdnOrganic(scale2(fdnOrganic))
                .alertLevel(level)
                .alertExplanation(alertExplanation)
                .nue(scale2(nue))
                .nOutput(scale2(nOutput))
                .nSurplus(scale2(nSurplus))
                .inputsBreakdown(breakdown)
                .sourceMethod(sourceMethod)
                .missingInputs(missingInputs)
                .notes(notes)
                .sustainabilityScore(scale2(sustainabilityScore))
                .sustainabilityLabel(sustainabilityLabel)
                .scoreComponents(componentScores)
                .scoreWeights(toWeightMap(weights))
                .thresholdSource(cfg.getThresholdSource())
                .yieldValue(yieldForCard)
                .yieldUnit(yieldUnit)
                .unit(unit)
                .yieldObserved(yieldObserved)
                .usedDefaultCropNContent(usedDefaultCropNContent)
                .build();
    }

    private Map<NutrientInputSource, BigDecimal> initInputMap() {
        Map<NutrientInputSource, BigDecimal> map = new LinkedHashMap<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            map.put(source, ZERO);
        }
        return map;
    }

    private Map<NutrientInputSource, String> initSourceMethodMap() {
        Map<NutrientInputSource, String> map = new LinkedHashMap<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            map.put(source, "missing");
        }
        return map;
    }

    private void applyExplicitInputEvents(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod
    ) {
        nutrientInputEventRepository.findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId())
                .forEach(event -> {
                    NutrientInputSource source = event.getInputSource();
                    if (source == null) {
                        return;
                    }

                    BigDecimal value = Optional.ofNullable(event.getNKg()).orElse(ZERO);
                    inputKg.put(source, inputKg.get(source).add(value));

                    boolean measured = Boolean.TRUE.equals(event.getMeasured())
                            || (event.getSourceType() != null && event.getSourceType().isMeasured());
                    sourceMethod.compute(source, (k, existing) -> mergeMethod(existing, measured ? "measured" : "estimated"));
                });
    }

    private void applyDedicatedIrrigationWaterAnalyses(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            List<String> notes
    ) {
        List<IrrigationWaterAnalysis> analyses = irrigationWaterAnalysisRepository
                .findAllBySeason_IdAndPlot_Id(season.getId(), plot.getId());
        if (analyses == null || analyses.isEmpty()) {
            return;
        }

        BigDecimal totalKg = ZERO;
        String method = "missing";
        boolean hasComputableRecord = false;

        for (IrrigationWaterAnalysis analysis : analyses) {
            BigDecimal contributionKg = irrigationContributionKg(analysis);
            if (contributionKg == null) {
                continue;
            }
            hasComputableRecord = true;
            totalKg = totalKg.add(contributionKg);
            method = mergeMethod(method, methodFromRecord(
                    analysis.getMeasured(),
                    analysis.getSourceType() != null && analysis.getSourceType().isMeasured()
            ));
        }

        if (!hasComputableRecord) {
            return;
        }

        inputKg.put(NutrientInputSource.IRRIGATION_WATER, totalKg);
        sourceMethod.put(NutrientInputSource.IRRIGATION_WATER, method);
        notes.add("Used dedicated irrigation-water-analysis records for irrigation water N contribution.");
    }

    private void applyDedicatedSoilTests(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            List<String> notes
    ) {
        List<SoilTest> soilTests = soilTestRepository
                .findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(season.getId(), plot.getId());
        if (soilTests == null || soilTests.isEmpty()) {
            return;
        }

        SoilTest latest = soilTests.stream()
                .filter(item -> item.getLegacyNContributionKg() != null || item.getMineralNKgPerHa() != null)
                .findFirst()
                .orElse(null);
        if (latest == null) {
            return;
        }

        BigDecimal soilLegacyKg;
        if (latest.getLegacyNContributionKg() != null) {
            soilLegacyKg = latest.getLegacyNContributionKg();
        } else {
            BigDecimal area = positiveOrNull(plot.getArea());
            if (area == null) {
                sourceMethod.put(NutrientInputSource.SOIL_LEGACY, "unavailable");
                inputKg.put(NutrientInputSource.SOIL_LEGACY, ZERO);
                notes.add("Soil test exists but plot area is missing, so soil legacy contribution is unavailable.");
                return;
            }
            soilLegacyKg = latest.getMineralNKgPerHa().multiply(area);
        }

        inputKg.put(NutrientInputSource.SOIL_LEGACY, soilLegacyKg);
        sourceMethod.put(
                NutrientInputSource.SOIL_LEGACY,
                methodFromRecord(
                        latest.getMeasured(),
                        latest.getSourceType() != null && latest.getSourceType().isMeasured()
                )
        );
        notes.add("Used dedicated soil-test domain record as soil legacy N contribution source.");
    }

    private BigDecimal irrigationContributionKg(IrrigationWaterAnalysis analysis) {
        if (analysis == null) {
            return null;
        }
        if (analysis.getLegacyNContributionKg() != null) {
            return analysis.getLegacyNContributionKg();
        }
        BigDecimal concentration = analysis.getTotalNmgPerL();
        if (concentration == null) {
            BigDecimal nitrate = safe(analysis.getNitrateMgPerL());
            BigDecimal ammonium = safe(analysis.getAmmoniumMgPerL());
            if (analysis.getNitrateMgPerL() == null && analysis.getAmmoniumMgPerL() == null) {
                return null;
            }
            concentration = nitrate.add(ammonium);
        }
        BigDecimal volumeM3 = analysis.getIrrigationVolumeM3();
        if (volumeM3 == null) {
            return null;
        }
        return concentration.multiply(volumeM3).divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
    }

    private String methodFromRecord(Boolean measuredFlag, boolean measuredBySourceType) {
        boolean measured = Boolean.TRUE.equals(measuredFlag) || measuredBySourceType;
        return measured ? "measured" : "estimated";
    }

    private void estimateFertilizerFromExpenses(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            AppProperties.EstimationDefaults est,
            List<String> notes
    ) {
        boolean hasMineralEvent = !"missing".equals(sourceMethod.get(NutrientInputSource.MINERAL_FERTILIZER));
        boolean hasOrganicEvent = !"missing".equals(sourceMethod.get(NutrientInputSource.ORGANIC_FERTILIZER));
        if (hasMineralEvent || hasOrganicEvent) {
            return;
        }

        List<ExpenseQueryPort.ExpenseFertilizerSnapshot> fertilizerExpenses =
                expenseQueryPort.findFertilizerExpensesBySeasonId(season.getId());
        if (fertilizerExpenses.isEmpty()) {
            notes.add("No fertilizer expenses found for fallback estimation.");
            return;
        }

        BigDecimal mineralEstimate = ZERO;
        BigDecimal organicEstimate = ZERO;
        for (ExpenseQueryPort.ExpenseFertilizerSnapshot expense : fertilizerExpenses) {
            if (expense.quantity() == null || expense.quantity() <= 0) {
                continue;
            }
            BigDecimal quantity = BigDecimal.valueOf(expense.quantity());
            String fingerprint = (nullToEmpty(expense.itemName()) + " " + nullToEmpty(expense.note()))
                    .toLowerCase(Locale.ROOT);
            if (containsAny(fingerprint, ORGANIC_KEYWORDS)) {
                organicEstimate = organicEstimate.add(quantity.multiply(safe(est.getOrganicFertilizerNRatio())));
            } else {
                mineralEstimate = mineralEstimate.add(quantity.multiply(safe(est.getMineralFertilizerNRatio())));
            }
        }

        if (mineralEstimate.compareTo(ZERO) > 0) {
            inputKg.put(NutrientInputSource.MINERAL_FERTILIZER, inputKg.get(NutrientInputSource.MINERAL_FERTILIZER).add(mineralEstimate));
            sourceMethod.put(NutrientInputSource.MINERAL_FERTILIZER, "estimated");
        }
        if (organicEstimate.compareTo(ZERO) > 0) {
            inputKg.put(NutrientInputSource.ORGANIC_FERTILIZER, inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER).add(organicEstimate));
            sourceMethod.put(NutrientInputSource.ORGANIC_FERTILIZER, "estimated");
        }
        if (mineralEstimate.compareTo(ZERO) > 0 || organicEstimate.compareTo(ZERO) > 0) {
            notes.add("Estimated fertilizer N from expense quantity because explicit nutrient events were missing.");
        }
    }

    private void estimateBiologicalFixation(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            BigDecimal areaHa,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            AppProperties.EstimationDefaults est,
            List<String> notes
    ) {
        if (!"missing".equals(sourceMethod.get(NutrientInputSource.BIOLOGICAL_FIXATION))) {
            return;
        }
        if (areaHa == null) {
            return;
        }

        String cropName = season.getCrop() != null ? nullToEmpty(season.getCrop().getCropName()).toLowerCase(Locale.ROOT) : "";
        if (!containsAny(cropName, LEGUME_KEYWORDS)) {
            inputKg.put(NutrientInputSource.BIOLOGICAL_FIXATION, ZERO);
            sourceMethod.put(NutrientInputSource.BIOLOGICAL_FIXATION, "estimated");
            return;
        }

        BigDecimal estimated = areaHa.multiply(safe(est.getLegumeFixationKgPerHa()));
        inputKg.put(NutrientInputSource.BIOLOGICAL_FIXATION, estimated);
        sourceMethod.put(NutrientInputSource.BIOLOGICAL_FIXATION, "estimated");
        notes.add("Estimated biological fixation from legume crop rule.");
    }

    private void estimateIrrigationWater(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            BigDecimal areaHa,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            AppProperties.EstimationDefaults est,
            List<String> notes
    ) {
        if (!"missing".equals(sourceMethod.get(NutrientInputSource.IRRIGATION_WATER))) {
            return;
        }
        if (areaHa == null) {
            return;
        }

        long irrigationEvents = seasonQueryPort.countFieldLogsBySeasonAndLogType(season.getId(), "IRRIGATE");
        BigDecimal estimated = areaHa
                .multiply(BigDecimal.valueOf(irrigationEvents))
                .multiply(safe(est.getIrrigationWaterKgPerHaPerEvent()));

        inputKg.put(NutrientInputSource.IRRIGATION_WATER, estimated);
        sourceMethod.put(NutrientInputSource.IRRIGATION_WATER, "estimated");
        if (irrigationEvents > 0) {
            notes.add("Estimated irrigation water N from IRRIGATE logs.");
        }
    }

    private void estimateAtmosphericDeposition(
            BigDecimal areaHa,
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            AppProperties.EstimationDefaults est,
            List<String> notes
    ) {
        if (!"missing".equals(sourceMethod.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION))) {
            return;
        }
        if (areaHa == null) {
            return;
        }
        BigDecimal estimated = areaHa.multiply(safe(est.getAtmosphericDepositionKgPerHa()));
        inputKg.put(NutrientInputSource.ATMOSPHERIC_DEPOSITION, estimated);
        sourceMethod.put(NutrientInputSource.ATMOSPHERIC_DEPOSITION, "estimated");
        notes.add("Estimated atmospheric deposition from configurable default.");
    }

    private void estimateOptionalSources(
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            List<String> notes
    ) {
        if ("missing".equals(sourceMethod.get(NutrientInputSource.SEED_IMPORT))) {
            inputKg.put(NutrientInputSource.SEED_IMPORT, ZERO);
            sourceMethod.put(NutrientInputSource.SEED_IMPORT, "estimated");
            notes.add("Source SEED_IMPORT defaulted to 0 because no model input is available.");
        }
        if ("missing".equals(sourceMethod.get(NutrientInputSource.SOIL_LEGACY))) {
            notes.add("Soil legacy N is missing; record soil test input to improve confidence.");
        }
    }

    private String resolveCalculationMode(
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod
    ) {
        if (inputKg.get(NutrientInputSource.CONTROL_SUPPLY).compareTo(ZERO) > 0) {
            return "exact_control";
        }

        List<NutrientInputSource> required = List.of(
                NutrientInputSource.MINERAL_FERTILIZER,
                NutrientInputSource.ORGANIC_FERTILIZER,
                NutrientInputSource.BIOLOGICAL_FIXATION,
                NutrientInputSource.IRRIGATION_WATER,
                NutrientInputSource.ATMOSPHERIC_DEPOSITION
        );
        boolean hasEstimated = required.stream().anyMatch(source ->
                !"measured".equals(sourceMethod.get(source))
        );
        return hasEstimated ? "hybrid_estimated" : "explicit_budget";
    }

    private BigDecimal sumInputsByMode(Map<NutrientInputSource, BigDecimal> inputKg, String mode) {
        if ("exact_control".equals(mode)) {
            return inputKg.get(NutrientInputSource.MINERAL_FERTILIZER)
                    .add(inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER))
                    .add(inputKg.get(NutrientInputSource.CONTROL_SUPPLY));
        }
        return inputKg.get(NutrientInputSource.MINERAL_FERTILIZER)
                .add(inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER))
                .add(inputKg.get(NutrientInputSource.BIOLOGICAL_FIXATION))
                .add(inputKg.get(NutrientInputSource.IRRIGATION_WATER))
                .add(inputKg.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION))
                .add(inputKg.get(NutrientInputSource.SEED_IMPORT))
                .add(inputKg.get(NutrientInputSource.SOIL_LEGACY));
    }

    private BigDecimal resolveCropNContent(org.example.QuanLyMuaVu.module.season.entity.Season season, AppProperties.EstimationDefaults est, List<String> notes) {
        if (season.getCrop() != null && season.getCrop().getId() != null) {
            Optional<org.example.QuanLyMuaVu.module.cropcatalog.entity.CropNitrogenReference> found = cropCatalogQueryPort
                    .findActiveNitrogenReferenceByCropId(season.getCrop().getId());
            if (found.isPresent()) {
                return safe(found.get().getNContentKgPerKgYield());
            }
        }
        notes.add("Using default crop N concentration because crop-specific reference is missing.");
        return safe(est.getDefaultCropNContentKgPerKgYield());
    }

    private String resolveAlertLevel(BigDecimal fdnTotal, AppProperties.AlertThresholds thresholds) {
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

    private String resolveAlertExplanation(String level, AppProperties.AlertThresholds thresholds) {
        return switch (level) {
            case "low" -> "FDN is below configured low threshold (" + thresholds.getLowMaxExclusive() + "%).";
            case "high" -> "FDN is above configured medium threshold (" + thresholds.getMediumMaxExclusive() + "%).";
            default -> "FDN is between configured low and high thresholds.";
        };
    }

    private List<String> collectMissingInputs(Map<NutrientInputSource, String> sourceMethod) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<NutrientInputSource, String> entry : sourceMethod.entrySet()) {
            if ("missing".equals(entry.getValue())) {
                missing.add(entry.getKey().name());
            }
        }
        return missing;
    }

    private BigDecimal calculateConfidence(
            Map<NutrientInputSource, String> sourceMethod,
            boolean hasArea,
            BigDecimal nContent,
            AppProperties.EstimationDefaults est
    ) {
        double score = 1.0;
        List<NutrientInputSource> required = List.of(
                NutrientInputSource.MINERAL_FERTILIZER,
                NutrientInputSource.ORGANIC_FERTILIZER,
                NutrientInputSource.BIOLOGICAL_FIXATION,
                NutrientInputSource.IRRIGATION_WATER,
                NutrientInputSource.ATMOSPHERIC_DEPOSITION,
                NutrientInputSource.SOIL_LEGACY
        );
        for (NutrientInputSource source : required) {
            String method = normalizeMethod(sourceMethod.get(source));
            if ("missing".equals(method)) {
                score -= missingPenalty(source);
            } else if ("estimated".equals(method)) {
                score -= estimatedPenalty(source);
            } else if ("mixed".equals(method)) {
                score -= mixedPenalty(source);
            } else if ("unavailable".equals(method)) {
                score -= unavailablePenalty(source);
            }
        }
        if (!hasArea) {
            score -= 0.15;
        }
        if (nContent.compareTo(safe(est.getDefaultCropNContentKgPerKgYield())) == 0) {
            score -= 0.08;
        }
        score = Math.max(0.10, Math.min(1.0, score));
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeMethod(String method) {
        return method == null ? "missing" : method.trim().toLowerCase(Locale.ROOT);
    }

    private double missingPenalty(NutrientInputSource source) {
        return source == NutrientInputSource.SOIL_LEGACY ? 0.08 : 0.20;
    }

    private double estimatedPenalty(NutrientInputSource source) {
        return source == NutrientInputSource.SOIL_LEGACY ? 0.05 : 0.12;
    }

    private double mixedPenalty(NutrientInputSource source) {
        return source == NutrientInputSource.SOIL_LEGACY ? 0.03 : 0.08;
    }

    private double unavailablePenalty(NutrientInputSource source) {
        return source == NutrientInputSource.SOIL_LEGACY ? 0.04 : 0.10;
    }

    private Map<String, BigDecimal> buildComponentScores(
            BigDecimal fdnTotal,
            BigDecimal nue,
            BigDecimal nSurplusKg,
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            BigDecimal actualHarvestKg,
            BigDecimal confidence
    ) {
        Map<String, BigDecimal> components = new LinkedHashMap<>();

        BigDecimal dependencyScore = fdnTotal == null ? ZERO : max(ZERO, HUNDRED.subtract(fdnTotal));
        BigDecimal efficiencyScore = clampPercent(nue);
        BigDecimal productivityScore = buildProductivityScore(season, actualHarvestKg);
        BigDecimal riskScore = buildRiskScore(nSurplusKg);
        BigDecimal confidenceScore = clampPercent(confidence.multiply(HUNDRED));

        components.put("dependency", scale2(dependencyScore));
        components.put("efficiency", scale2(efficiencyScore));
        components.put("productivity", scale2(productivityScore));
        components.put("risk", scale2(riskScore));
        components.put("confidence", scale2(confidenceScore));
        return components;
    }

    private BigDecimal buildProductivityScore(org.example.QuanLyMuaVu.module.season.entity.Season season, BigDecimal actualHarvestKg) {
        if (season == null || season.getExpectedYieldKg() == null || season.getExpectedYieldKg().compareTo(ZERO) <= 0) {
            return actualHarvestKg.compareTo(ZERO) > 0 ? BigDecimal.valueOf(60) : BigDecimal.valueOf(40);
        }
        BigDecimal ratio = actualHarvestKg.multiply(HUNDRED)
                .divide(season.getExpectedYieldKg(), 2, RoundingMode.HALF_UP);
        return clampPercent(ratio);
    }

    private BigDecimal buildRiskScore(BigDecimal nSurplusKg) {
        if (nSurplusKg == null) {
            return BigDecimal.valueOf(50);
        }
        if (nSurplusKg.compareTo(ZERO) <= 0) {
            return HUNDRED;
        }
        BigDecimal penalty = nSurplusKg.multiply(new BigDecimal("1.5"));
        return clampPercent(HUNDRED.subtract(penalty));
    }

    private BigDecimal computeScore(Map<String, BigDecimal> components, AppProperties.ScoreWeights weights) {
        Map<String, BigDecimal> w = toWeightMap(weights);
        BigDecimal weightSum = w.values().stream().reduce(ZERO, BigDecimal::add);
        if (weightSum.compareTo(ZERO) <= 0) {
            return ZERO;
        }

        BigDecimal weightedSum = ZERO;
        weightedSum = weightedSum.add(components.get("dependency").multiply(w.get("dependency")));
        weightedSum = weightedSum.add(components.get("efficiency").multiply(w.get("efficiency")));
        weightedSum = weightedSum.add(components.get("productivity").multiply(w.get("productivity")));
        weightedSum = weightedSum.add(components.get("risk").multiply(w.get("risk")));
        weightedSum = weightedSum.add(components.get("confidence").multiply(w.get("confidence")));

        return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> toWeightMap(AppProperties.ScoreWeights weights) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("dependency", safe(weights.getDependency()));
        map.put("efficiency", safe(weights.getEfficiency()));
        map.put("productivity", safe(weights.getProductivity()));
        map.put("risk", safe(weights.getRisk()));
        map.put("confidence", safe(weights.getConfidence()));
        return map;
    }

    private String mapScoreLabel(BigDecimal score) {
        if (score == null) {
            return "Needs optimization";
        }
        if (score.compareTo(BigDecimal.valueOf(50)) < 0) {
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

    private SustainabilityOverviewResponse.InputsBreakdown buildBreakdown(
            Map<NutrientInputSource, BigDecimal> inputKg,
            Map<NutrientInputSource, String> sourceMethod,
            BigDecimal areaHa
    ) {
        return SustainabilityOverviewResponse.InputsBreakdown.builder()
                .mineralFertilizerN(toBreakdownValue(
                        NutrientInputSource.MINERAL_FERTILIZER,
                        inputKg.get(NutrientInputSource.MINERAL_FERTILIZER),
                        sourceMethod,
                        areaHa
                ))
                .organicFertilizerN(toBreakdownValue(
                        NutrientInputSource.ORGANIC_FERTILIZER,
                        inputKg.get(NutrientInputSource.ORGANIC_FERTILIZER),
                        sourceMethod,
                        areaHa
                ))
                .biologicalFixationN(toBreakdownValue(
                        NutrientInputSource.BIOLOGICAL_FIXATION,
                        inputKg.get(NutrientInputSource.BIOLOGICAL_FIXATION),
                        sourceMethod,
                        areaHa
                ))
                .irrigationWaterN(toBreakdownValue(
                        NutrientInputSource.IRRIGATION_WATER,
                        inputKg.get(NutrientInputSource.IRRIGATION_WATER),
                        sourceMethod,
                        areaHa
                ))
                .atmosphericDepositionN(toBreakdownValue(
                        NutrientInputSource.ATMOSPHERIC_DEPOSITION,
                        inputKg.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION),
                        sourceMethod,
                        areaHa
                ))
                .seedImportN(toBreakdownValue(
                        NutrientInputSource.SEED_IMPORT,
                        inputKg.get(NutrientInputSource.SEED_IMPORT),
                        sourceMethod,
                        areaHa
                ))
                .soilLegacyN(toBreakdownValue(
                        NutrientInputSource.SOIL_LEGACY,
                        inputKg.get(NutrientInputSource.SOIL_LEGACY),
                        sourceMethod,
                        areaHa
                ))
                .controlSupplyN(toBreakdownValue(
                        NutrientInputSource.CONTROL_SUPPLY,
                        inputKg.get(NutrientInputSource.CONTROL_SUPPLY),
                        sourceMethod,
                        areaHa
                ))
                .build();
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) <= 0) {
            return null;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal toPerUnit(BigDecimal valueKg, BigDecimal areaHa) {
        if (valueKg == null) {
            return null;
        }
        if (areaHa == null || areaHa.compareTo(ZERO) <= 0) {
            return valueKg;
        }
        return valueKg.divide(areaHa, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal toBreakdownValue(
            NutrientInputSource source,
            BigDecimal valueKg,
            Map<NutrientInputSource, String> sourceMethod,
            BigDecimal areaHa
    ) {
        String method = sourceMethod != null ? sourceMethod.get(source) : null;
        if ("missing".equals(method) || "unavailable".equals(method)) {
            return null;
        }
        return scale2(toPerUnit(valueKg, areaHa));
    }

    private BigDecimal clampPercent(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return max(ZERO, min(HUNDRED, value));
    }

    private BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private BigDecimal scale2(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.compareTo(ZERO) > 0 ? value : null;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(ZERO) == 0;
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String mergeMethod(String current, String incoming) {
        if (current == null || "missing".equals(current)) {
            return incoming;
        }
        if (current.equals(incoming)) {
            return current;
        }
        return "mixed";
    }

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CalculationResult {
        String calculationMode;
        BigDecimal confidence;
        BigDecimal fdnTotal;
        BigDecimal fdnMineral;
        BigDecimal fdnOrganic;
        String alertLevel;
        String alertExplanation;
        BigDecimal nue;
        BigDecimal nOutput;
        BigDecimal nSurplus;
        SustainabilityOverviewResponse.InputsBreakdown inputsBreakdown;
        Map<NutrientInputSource, String> sourceMethod;
        List<String> missingInputs;
        List<String> notes;
        BigDecimal sustainabilityScore;
        String sustainabilityLabel;
        Map<String, BigDecimal> scoreComponents;
        Map<String, BigDecimal> scoreWeights;
        String thresholdSource;
        BigDecimal yieldValue;
        String yieldUnit;
        String unit;
        Boolean yieldObserved;
        Boolean usedDefaultCropNContent;

        public static CalculationResult empty() {
            Map<NutrientInputSource, String> missingSourceMap = new LinkedHashMap<>();
            for (NutrientInputSource source : NutrientInputSource.values()) {
                missingSourceMap.put(source, "missing");
            }
            return CalculationResult.builder()
                    .calculationMode("hybrid_estimated")
                    .confidence(BigDecimal.valueOf(0.10))
                    .fdnTotal(null)
                    .fdnMineral(null)
                    .fdnOrganic(null)
                    .alertLevel("medium")
                    .alertExplanation("No season or plot context available.")
                    .nue(null)
                    .nOutput(null)
                    .nSurplus(null)
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
                    .sourceMethod(missingSourceMap)
                    .missingInputs(missingSourceMap.keySet().stream().map(Enum::name).toList())
                    .notes(List.of("No data available for this context."))
                    .sustainabilityScore(null)
                    .sustainabilityLabel("Needs optimization")
                    .scoreComponents(Map.of())
                    .scoreWeights(Map.of())
                    .thresholdSource("config_default_v1")
                    .yieldValue(null)
                    .yieldUnit("t/ha")
                    .unit("kg N/ha/season")
                    .yieldObserved(false)
                    .usedDefaultCropNContent(false)
                    .build();
        }
    }
}

