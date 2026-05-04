package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
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
public class FieldMapResponse {

    List<FieldMapItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FieldMapItem {
        Integer fieldId;
        String fieldName;
        Integer farmId;
        String farmName;
        JsonNode geometry;
        LatLng center;
        String cropName;
        String seasonName;
        String fdnLevel;
        BigDecimal fdnTotal;
        BigDecimal fdnMineral;
        BigDecimal fdnOrganic;
        BigDecimal nue;
        BigDecimal confidence;
        String calculationMode;
        String thresholdSource;
        String recommendationSource;
        List<String> missingInputs;
        SustainabilityOverviewResponse.InputsBreakdown inputsBreakdown;
        List<String> recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LatLng {
        BigDecimal lat;
        BigDecimal lng;
    }
}
