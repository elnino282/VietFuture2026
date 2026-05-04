package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SustainabilityOverviewResponse {

    String scope;
    String entityId;
    Integer seasonId;
    String calculationMode;
    BigDecimal confidence;
    SustainabilityScore sustainableScore;
    FdnMetrics fdn;
    BigDecimal nue;
    @JsonProperty("nOutput")
    BigDecimal nOutput;
    @JsonProperty("nSurplus")
    BigDecimal nSurplus;
    CurrentSeason currentSeason;
    YieldSummary yield;
    InputsBreakdown inputsBreakdown;
    String unit;
    List<DataInputQuality> dataQuality;
    DataQualitySummary dataQualitySummary;
    List<String> missingInputs;
    List<String> notes;
    List<String> recommendations;
    String recommendationSource;
    List<HistoryPoint> historicalTrend;
    MetricValue sustainableScoreMetric;
    MetricValue fdnTotalMetric;
    MetricValue fdnMineralMetric;
    MetricValue fdnOrganicMetric;
    MetricValue nueMetric;
    @JsonProperty("nOutputMetric")
    MetricValue nOutputMetric;
    @JsonProperty("nSurplusMetric")
    MetricValue nSurplusMetric;
    MetricValue estimatedYieldMetric;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SustainabilityScore {
        BigDecimal value;
        String label;
        Map<String, BigDecimal> components;
        Map<String, BigDecimal> weights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FdnMetrics {
        BigDecimal total;
        BigDecimal mineral;
        BigDecimal organic;
        String level;
        String status;
        String thresholdSource;
        BigDecimal lowMaxExclusive;
        BigDecimal mediumMaxExclusive;
        BigDecimal mineralHighMin;
        String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CurrentSeason {
        String seasonName;
        String cropName;
        Integer dayCount;
        String stage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class YieldSummary {
        BigDecimal estimated;
        String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InputsBreakdown {
        BigDecimal mineralFertilizerN;
        BigDecimal organicFertilizerN;
        BigDecimal biologicalFixationN;
        BigDecimal irrigationWaterN;
        BigDecimal atmosphericDepositionN;
        BigDecimal seedImportN;
        BigDecimal soilLegacyN;
        BigDecimal controlSupplyN;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DataInputQuality {
        String source;
        String method;
        BigDecimal confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DataQualitySummary {
        BigDecimal overallConfidence;
        Integer measuredInputCount;
        Integer estimatedInputCount;
        Integer missingInputCount;
        Integer unavailableInputCount;
        String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MetricValue {
        BigDecimal value;
        String unit;
        String status;
        BigDecimal confidence;
        String calculationMode;
        List<String> assumptions;
        List<String> missingInputs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class HistoryPoint {
        Integer seasonId;
        String seasonName;
        LocalDate startDate;
        BigDecimal fdnTotal;
        BigDecimal fdnMineral;
        BigDecimal fdnOrganic;
        BigDecimal nue;
        @JsonProperty("nOutput")
        BigDecimal nOutput;
        BigDecimal yield;
    }
}
