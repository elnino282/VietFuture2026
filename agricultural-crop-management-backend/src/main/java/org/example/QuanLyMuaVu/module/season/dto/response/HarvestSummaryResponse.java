package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestSummaryResponse {
    BigDecimal totalHarvestedKg;
    Integer lotsCount;
    BigDecimal totalRevenue;
    BigDecimal yieldVsPlanPercent;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
    BigDecimal totalStoredKg;
    BigDecimal totalSoldKg;
    BigDecimal totalProcessingKg;
    BigDecimal premiumGradePercentage;
}
