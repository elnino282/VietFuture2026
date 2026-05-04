package org.example.QuanLyMuaVu.module.sustainability.service;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SustainabilityRecommendationService {

    static final BigDecimal ZERO = BigDecimal.ZERO;
    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    AppProperties appProperties;

    public List<String> generate(SustainabilityCalculationService.CalculationResult result) {
        if (result == null) {
            return List.of("Collect complete nutrient input and harvest data before applying strong optimization actions.");
        }
        return generate(
                result.getFdnTotal(),
                result.getFdnMineral(),
                result.getNue(),
                result.getNSurplus(),
                result.getConfidence(),
                result.getInputsBreakdown(),
                result.getSourceMethod()
        );
    }

    public List<String> generate(
            BigDecimal fdnTotal,
            BigDecimal fdnMineral,
            BigDecimal nue,
            BigDecimal nSurplus,
            BigDecimal confidence,
            SustainabilityOverviewResponse.InputsBreakdown inputsBreakdown,
            Map<NutrientInputSource, String> sourceMethod
    ) {
        AppProperties.Sustainability cfg = appProperties.getSustainability() != null
                ? appProperties.getSustainability()
                : new AppProperties.Sustainability();
        AppProperties.AlertThresholds thresholds = cfg.getAlerts() != null
                ? cfg.getAlerts()
                : new AppProperties.AlertThresholds();
        AppProperties.EstimationDefaults est = cfg.getEstimation() != null
                ? cfg.getEstimation()
                : new AppProperties.EstimationDefaults();

        Set<String> suggestions = new LinkedHashSet<>();

        boolean mineralHigh = fdnMineral != null
                && fdnMineral.compareTo(safe(thresholds.getMineralHighMin())) >= 0;
        boolean confidenceStrong = confidence != null
                && confidence.compareTo(safe(est.getStrongActionMinConfidence())) >= 0;
        boolean nueSafe = nue != null && nue.compareTo(safe(est.getStrongActionMinNue())) >= 0;

        if (mineralHigh) {
            if (confidenceStrong && nueSafe) {
                suggestions.add("Consider reducing mineral nitrogen application by around 20% and monitor yield response before scaling this rule.");
            } else {
                suggestions.add("Mineral fertilizer dependency is high, but data confidence or NUE is not strong enough for an automatic 20% mineral-N reduction.");
            }
            suggestions.add("Increase the share of organic inputs (compost/manure) to replace part of mineral nitrogen.");
            suggestions.add("Check irrigation-water nitrogen first before scheduling additional mineral fertilizer.");
        }

        if (fdnTotal != null && fdnTotal.compareTo(safe(thresholds.getMediumMaxExclusive())) >= 0) {
            suggestions.add("Plan legume rotation or intercropping to increase biological nitrogen contribution next season.");
            suggestions.add("Prioritize circular nutrient sources from local residues when available.");
        }

        if (nSurplus != null && nSurplus.compareTo(BigDecimal.valueOf(25)) > 0) {
            suggestions.add("Nitrogen surplus is elevated; split fertilizer timing and calibrate doses by growth stage.");
        }

        if (inputsBreakdown != null && sharePercent(
                inputsBreakdown.getOrganicFertilizerN(),
                totalInputs(inputsBreakdown)
        ).compareTo(BigDecimal.valueOf(15)) < 0) {
            suggestions.add("Organic nitrogen share is low; evaluate compost/manure substitution in the next input plan.");
        }

        if (sourceMethod != null) {
            String irrigationMethod = normalize(sourceMethod.get(NutrientInputSource.IRRIGATION_WATER));
            if (!"measured".equals(irrigationMethod)) {
                suggestions.add("Irrigation water nitrogen is not measured; run water analysis to reduce fertilizer over-application risk.");
            }
            String soilMethod = normalize(sourceMethod.get(NutrientInputSource.SOIL_LEGACY));
            if (!"measured".equals(soilMethod)) {
                suggestions.add("Soil test data is incomplete; measure mineral-N/organic matter to improve confidence before aggressive nutrient adjustments.");
            }
            if ("estimated".equals(normalize(sourceMethod.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION)))
                    || "estimated".equals(normalize(sourceMethod.get(NutrientInputSource.BIOLOGICAL_FIXATION)))) {
                suggestions.add("Key non-fertilizer nitrogen sources are estimated; improve field records to increase confidence.");
            }
        }

        if (confidence != null && confidence.compareTo(new BigDecimal("0.60")) < 0) {
            suggestions.add("Current metrics rely on estimated inputs; improve measured records before applying aggressive optimization.");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Maintain current nutrient strategy and continue monitoring FDN, NUE, and output trends.");
        }

        return suggestions.stream().toList();
    }

    private BigDecimal totalInputs(SustainabilityOverviewResponse.InputsBreakdown inputs) {
        if (inputs == null) {
            return ZERO;
        }
        return safe(inputs.getMineralFertilizerN())
                .add(safe(inputs.getOrganicFertilizerN()))
                .add(safe(inputs.getBiologicalFixationN()))
                .add(safe(inputs.getIrrigationWaterN()))
                .add(safe(inputs.getAtmosphericDepositionN()))
                .add(safe(inputs.getSeedImportN()))
                .add(safe(inputs.getSoilLegacyN()))
                .add(safe(inputs.getControlSupplyN()));
    }

    private BigDecimal sharePercent(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return value.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
