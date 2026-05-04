package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class AdminReportResponse {

    // ═══════════════════════════════════════════════════════════════
    // LEGACY DTOs (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTotal {
        private Integer year;
        private Integer month;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonHarvest {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentsSummary {
        private Map<String, Long> bySeverity;
        private Map<String, Long> byStatus;
        private Long totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementSummary {
        private Integer year;
        private Integer month;
        private String movementType;
        private BigDecimal totalQuantity;
    }

    // ═══════════════════════════════════════════════════════════════
    // NEW ANALYTICS DTOs
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yield Report: Compare expected vs actual yield by season/crop/plot.
     * variancePercent = (actualYieldKg - expectedYieldKg) / expectedYieldKg * 100
     * NULL if expectedYieldKg is 0 or null.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private String plotName;
        private String farmName;
        private BigDecimal expectedYieldKg;
        private BigDecimal actualYieldKg;
        private BigDecimal variancePercent; // nullable
    }

    /**
     * Cost Report: Total expenses per season with cost/kg calculation.
     * costPerKg = totalExpense / totalYieldKg
     * NULL if totalYieldKg is 0 (no harvest yet).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalExpense;
        private BigDecimal totalYieldKg;
        private BigDecimal costPerKg; // nullable
    }

    /**
     * Revenue Report: Total revenue from harvests.
     * harvests.unit = price per unit (VND per kg)
     * totalRevenue = SUM(quantity * unit)
     * avgPricePerUnit = totalRevenue / totalQuantity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal marketplaceRevenue; // nullable until marketplace contract is provided
        private String marketplaceRevenueStatus; // e.g. TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT
        private BigDecimal avgPricePerUnit; // nullable if no quantity
    }

    /**
     * Profit Report: Combined revenue and expense analysis.
     * grossProfit = totalRevenue - totalExpense
     * profitMargin = (grossProfit / totalRevenue) * 100, nullable if no revenue
     * returnOnCost = (grossProfit / totalExpense) * 100, nullable if no expense
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitReport {
        private Integer seasonId;
        private String seasonName;
        private String cropName;
        private String farmName;
        private BigDecimal totalRevenue; // scale 0 (VND)
        private BigDecimal marketplaceRevenue; // nullable until marketplace contract is provided
        private String marketplaceRevenueStatus; // e.g. TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT
        private BigDecimal totalExpense; // scale 0 (VND)
        private BigDecimal grossProfit; // scale 0 (VND)
        private BigDecimal profitMargin; // scale 2 (%), nullable if no revenue
        private BigDecimal returnOnCost; // scale 2 (%), nullable if no expense
    }

    /**
     * Filters returned with report summary for traceability.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedFilters {
        private String dateFrom;
        private String dateTo;
        private Integer cropId;
        private Integer farmId;
        private Integer plotId;
        private Integer varietyId;
        private BigDecimal areaMinHa;
        private BigDecimal areaMaxHa;
    }

    /**
     * Summary KPI payload for admin reports dashboard.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummary {
        private BigDecimal actualYield;
        private BigDecimal totalCost;
        private BigDecimal costPerTon;
        private BigDecimal revenue;
        private BigDecimal marketplaceRevenue; // nullable until marketplace contract is provided
        private String marketplaceRevenueStatus; // e.g. TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
        private List<String> warnings;
        private AppliedFilters appliedFilters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldAnalyticsRow {
        private Integer farmId;
        private String farmName;
        private Integer plotId;
        private String plotName;
        private Integer cropId;
        private String cropName;
        private Integer varietyId;
        private String varietyName;
        private BigDecimal actualYield;
        private Long harvestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldAnalyticsTotals {
        private BigDecimal actualYield;
        private Long harvestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YieldAnalyticsResponse {
        private List<YieldAnalyticsRow> tableRows;
        private List<YieldAnalyticsRow> chartSeries;
        private YieldAnalyticsTotals totals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostCategoryRow {
        private String category;
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostVendorRow {
        private Integer vendorId;
        private String vendorName;
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTimeRow {
        private String periodStart;
        private String label;
        private BigDecimal totalCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTotals {
        private BigDecimal totalCost;
        private Long expenseCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostAnalyticsResponse {
        private List<CostCategoryRow> tableRows;
        private List<CostCategoryRow> chartSeries;
        private CostTotals totals;
        private List<CostVendorRow> vendorRows;
        private List<CostTimeRow> timeSeries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueRow {
        private Integer cropId;
        private String cropName;
        private Integer plotId;
        private String plotName;
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal avgPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTotals {
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
        private BigDecimal marketplaceRevenue; // nullable until marketplace contract is provided
        private String marketplaceRevenueStatus; // e.g. TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT
        private BigDecimal avgPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueAnalyticsResponse {
        private List<RevenueRow> tableRows;
        private List<RevenueRow> chartSeries;
        private RevenueTotals totals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitRow {
        private Integer cropId;
        private String cropName;
        private Integer plotId;
        private String plotName;
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitTotals {
        private BigDecimal totalRevenue;
        private BigDecimal marketplaceRevenue; // nullable until marketplace contract is provided
        private String marketplaceRevenueStatus; // e.g. TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitAnalyticsResponse {
        private List<ProfitRow> tableRows;
        private List<ProfitRow> chartSeries;
        private ProfitTotals totals;
    }

    /**
     * Task Performance Report: Aggregated task metrics.
     * completionRate = completedTasks / totalTasks * 100
     * overdueRate = overdueTasks / totalTasks * 100
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskPerformanceReport {
        private Long totalTasks;
        private Long completedTasks;
        private Long overdueTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long cancelledTasks;
        private BigDecimal completionRate; // percentage
        private BigDecimal overdueRate; // percentage
    }

    /**
     * Inventory On-Hand Report: Current stock by warehouse.
     * No price data → just quantity summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryOnHandReport {
        private Integer warehouseId;
        private String warehouseName;
        private String farmName;
        private Integer totalLots;
        private BigDecimal totalQuantityOnHand;
        private Integer expiredLots;
        private Integer expiringSoonLots; // expiry within 30 days
    }

    /**
     * Incident Statistics Report: Breakdown by type, severity, status.
     * averageResolutionDays = AVG(DATEDIFF(resolved_at, created_at))
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentStatisticsReport {
        private Map<String, Long> byIncidentType;
        private Map<String, Long> bySeverity;
        private Map<String, Long> byStatus;
        private Long totalCount;
        private Long openCount;
        private Long resolvedCount;
        private BigDecimal averageResolutionDays; // nullable if no resolved incidents
    }
}
