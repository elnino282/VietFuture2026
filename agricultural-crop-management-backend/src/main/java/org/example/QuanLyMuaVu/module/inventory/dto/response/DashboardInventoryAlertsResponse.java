package org.example.QuanLyMuaVu.module.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class DashboardInventoryAlertsResponse {
    LocalDate asOfDate;
    BigDecimal lowStockThreshold;
    Integer expiringSoonDays;
    Integer noMovementDays;
    String thresholdSource;
    Summary summary;
    List<AlertItem> alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Summary {
        Integer totalAlerts;
        Integer lowStock;
        Integer expired;
        Integer expiringSoon;
        Integer noMovement;
        Integer abnormalMovement;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AlertItem {
        Integer supplyLotId;
        String itemName;
        String lotCode;
        String warehouseName;
        String locationLabel;
        BigDecimal quantity;
        String unit;
        LocalDate expiryDate;
        String alertType;
        String severity;
        String reason;
        LocalDateTime lastMovementAt;
    }
}
