package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminInventoryHealthResponse {
    LocalDate asOfDate;
    Integer windowDays;
    Boolean includeExpiring;
    Summary summary;
    DataQuality dataQuality;
    List<FarmHealth> farms;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Summary {
        Integer expiredLots;
        Integer expiringSoonLots;
        Integer expiringLots;
        Integer lowStockLots;
        Integer noMovementLots;
        Integer slowMovementLots;
        Double qtyAtRisk;
        Integer totalAffectedFarms;
        Integer totalAffectedItems;
        Integer unknownExpiryLots;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DataQuality {
        Integer missingExpiryDateCount;
        Integer missingMovementHistoryCount;
        Double coveragePercent;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FarmHealth {
        Integer farmId;
        String farmName;
        Integer expiredLots;
        Integer expiringSoonLots;
        Integer expiringLots;
        Integer lowStockLots;
        Integer noMovementLots;
        Integer slowMovementLots;
        Double qtyAtRisk;
        List<RiskLot> topRiskLots;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RiskLot {
        Integer lotId;
        String itemName;
        String expiryDate;
        Double onHand;
        String status;
    }
}
