package org.example.QuanLyMuaVu.module.sustainability.dto.response;

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
public class FieldRecommendationsResponse {
    Integer fieldId;
    Integer seasonId;
    BigDecimal fdnTotal;
    BigDecimal fdnMineral;
    BigDecimal nue;
    BigDecimal confidence;
    String fdnLevel;
    String thresholdSource;
    String recommendationSource;
    String calculationMode;
    List<String> missingInputs;
    List<String> recommendations;
}
