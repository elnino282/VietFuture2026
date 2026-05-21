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
import org.springframework.context.i18n.LocaleContextHolder;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SustainabilityRecommendationService {

    static final BigDecimal ZERO = BigDecimal.ZERO;
    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    AppProperties appProperties;

    public List<String> generate(SustainabilityCalculationService.CalculationResult result) {
        if (result == null) {
            return List.of(localize(
                    "Collect complete nutrient input and harvest data before applying strong optimization actions.",
                    "Cần bổ sung đầy đủ dữ liệu đầu vào đạm và dữ liệu thu hoạch trước khi áp dụng các hành động tối ưu hóa mạnh."
            ));
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
        boolean vietnamese = isVietnameseLocale();

        boolean mineralHigh = fdnMineral != null
                && fdnMineral.compareTo(safe(thresholds.getMineralHighMin())) >= 0;
        boolean confidenceStrong = confidence != null
                && confidence.compareTo(safe(est.getStrongActionMinConfidence())) >= 0;
        boolean nueSafe = nue != null && nue.compareTo(safe(est.getStrongActionMinNue())) >= 0;

        if (mineralHigh) {
            if (confidenceStrong && nueSafe) {
                suggestions.add(localize(
                        vietnamese,
                        "Consider reducing mineral nitrogen application by around 20% and monitor yield response before scaling this rule.",
                        "Cân nhắc giảm khoảng 20% lượng đạm vô cơ và theo dõi phản ứng năng suất trước khi mở rộng quy tắc này."
                ));
            } else {
                suggestions.add(localize(
                        vietnamese,
                        "Mineral fertilizer dependency is high, but data confidence or NUE is not strong enough for an automatic 20% mineral-N reduction.",
                        "Mức phụ thuộc phân vô cơ đang cao, nhưng độ tin cậy dữ liệu hoặc NUE chưa đủ mạnh để tự động giảm 20% đạm vô cơ."
                ));
            }
            suggestions.add(localize(
                    vietnamese,
                    "Increase the share of organic inputs (compost/manure) to replace part of mineral nitrogen.",
                    "Tăng tỷ lệ đầu vào hữu cơ (compost/phân chuồng) để thay thế một phần đạm vô cơ."
            ));
            suggestions.add(localize(
                    vietnamese,
                    "Check irrigation-water nitrogen first before scheduling additional mineral fertilizer.",
                    "Kiểm tra hàm lượng đạm trong nước tưới trước khi lên lịch bón thêm phân vô cơ."
            ));
        }

        if (fdnTotal != null && fdnTotal.compareTo(safe(thresholds.getMediumMaxExclusive())) >= 0) {
            suggestions.add(localize(
                    vietnamese,
                    "Plan legume rotation or intercropping to increase biological nitrogen contribution next season.",
                    "Lên kế hoạch luân canh hoặc xen canh cây họ đậu để tăng đóng góp đạm sinh học cho mùa vụ sau."
            ));
            suggestions.add(localize(
                    vietnamese,
                    "Prioritize circular nutrient sources from local residues when available.",
                    "Ưu tiên nguồn dinh dưỡng tuần hoàn từ phụ phẩm địa phương khi có sẵn."
            ));
        }

        if (nSurplus != null && nSurplus.compareTo(BigDecimal.valueOf(25)) > 0) {
            suggestions.add(localize(
                    vietnamese,
                    "Nitrogen surplus is elevated; split fertilizer timing and calibrate doses by growth stage.",
                    "Lượng đạm dư đang cao; hãy chia nhỏ thời điểm bón và hiệu chỉnh liều theo từng giai đoạn sinh trưởng."
            ));
        }

        if (inputsBreakdown != null && sharePercent(
                inputsBreakdown.getOrganicFertilizerN(),
                totalInputs(inputsBreakdown)
        ).compareTo(BigDecimal.valueOf(15)) < 0) {
            suggestions.add(localize(
                    vietnamese,
                    "Organic nitrogen share is low; evaluate compost/manure substitution in the next input plan.",
                    "Tỷ lệ đạm hữu cơ đang thấp; hãy đánh giá thay thế bằng compost/phân chuồng trong kế hoạch đầu vào tiếp theo."
            ));
        }

        if (sourceMethod != null) {
            String irrigationMethod = normalize(sourceMethod.get(NutrientInputSource.IRRIGATION_WATER));
            if (!"measured".equals(irrigationMethod)) {
                suggestions.add(localize(
                        vietnamese,
                        "Irrigation water nitrogen is not measured; run water analysis to reduce fertilizer over-application risk.",
                        "Đạm trong nước tưới chưa được đo; hãy phân tích nước để giảm rủi ro bón phân quá mức."
                ));
            }
            String soilMethod = normalize(sourceMethod.get(NutrientInputSource.SOIL_LEGACY));
            if (!"measured".equals(soilMethod)) {
                suggestions.add(localize(
                        vietnamese,
                        "Soil test data is incomplete; measure mineral-N/organic matter to improve confidence before aggressive nutrient adjustments.",
                        "Dữ liệu xét nghiệm đất chưa đầy đủ; cần đo đạm khoáng/chất hữu cơ để tăng độ tin cậy trước khi điều chỉnh dinh dưỡng mạnh."
                ));
            }
            if ("estimated".equals(normalize(sourceMethod.get(NutrientInputSource.ATMOSPHERIC_DEPOSITION)))
                    || "estimated".equals(normalize(sourceMethod.get(NutrientInputSource.BIOLOGICAL_FIXATION)))) {
                suggestions.add(localize(
                        vietnamese,
                        "Key non-fertilizer nitrogen sources are estimated; improve field records to increase confidence.",
                        "Một số nguồn đạm quan trọng ngoài phân bón đang là ước tính; hãy cải thiện nhật ký đồng ruộng để tăng độ tin cậy."
                ));
            }
        }

        if (confidence != null && confidence.compareTo(new BigDecimal("0.60")) < 0) {
            suggestions.add(localize(
                    vietnamese,
                    "Current metrics rely on estimated inputs; improve measured records before applying aggressive optimization.",
                    "Các chỉ số hiện tại còn phụ thuộc dữ liệu ước tính; cần cải thiện dữ liệu đo thực tế trước khi áp dụng tối ưu hóa mạnh."
            ));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(localize(
                    vietnamese,
                    "Maintain current nutrient strategy and continue monitoring FDN, NUE, and output trends.",
                    "Duy trì chiến lược dinh dưỡng hiện tại và tiếp tục theo dõi xu hướng FDN, NUE và đầu ra."
            ));
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

    private boolean isVietnameseLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    private String localize(String english, String vietnamese) {
        return localize(isVietnameseLocale(), english, vietnamese);
    }

    private String localize(boolean vietnameseLocale, String english, String vietnamese) {
        return vietnameseLocale ? vietnamese : english;
    }
}
