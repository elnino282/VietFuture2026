package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Main Dashboard Overview Response DTO
 * Contains all aggregated metrics for the farmer dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardOverviewResponse {

    SeasonContext seasonContext;
    Counts counts;
    Kpis kpis;
    Expenses expenses;
    Harvest harvest;
    Alerts alerts;
    TaskStatusSummary taskStatus;
    InventoryRisk inventoryRisk;
    LotStatus lotStatus;
    SustainabilityAlerts sustainabilityAlerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SeasonContext {
        Integer seasonId;
        String seasonName;
        LocalDate startDate;
        LocalDate endDate;
        LocalDate plannedHarvestDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Counts {
        Integer activeFarms;
        Integer activePlots;
        Map<String, Integer> seasonsByStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Kpis {
        BigDecimal avgYieldTonsPerHa;
        BigDecimal costPerHectare;
        BigDecimal onTimePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Expenses {
        BigDecimal totalExpense;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Harvest {
        BigDecimal totalQuantityKg;
        BigDecimal totalRevenue;
        BigDecimal expectedYieldKg;
        BigDecimal yieldVsPlanPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Alerts {
        Integer openIncidents;
        Integer expiringLots;
        Integer lowStockItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TaskStatusSummary {
        Integer totalTasks;
        Integer pendingTasks;
        Integer inProgressTasks;
        Integer completedTasks;
        Integer overdueTasks;
        Integer cancelledTasks;
        Map<String, Integer> byStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InventoryRisk {
        Integer atRiskLots;
        Integer lowStockLots;
        Integer criticalLowStockLots;
        Integer expiringSoonLots;
        Integer expiredLots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LotStatus {
        Integer totalLotsWithStock;
        Integer activeLots;
        Integer expiringSoonLots;
        Integer expiredLots;
        Integer unknownExpiryLots;
        Map<String, Integer> byStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SustainabilityAlerts {
        Integer totalFields;
        Integer highRiskFields;
        Integer mediumRiskFields;
        Integer lowRiskFields;
        Integer fieldsMissingInputs;
    }
}
