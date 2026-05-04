package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for Dashboard KPI calculations.
 * Single Responsibility: Computing performance metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardKpiService {

    private final TaskQueryPort taskQueryPort;
    private final ExpenseQueryPort expenseQueryPort;
    private final HarvestQueryPort harvestQueryPort;

    /**
     * Build KPIs for a season.
     */
    public DashboardOverviewResponse.Kpis buildKpis(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null) {
            return DashboardOverviewResponse.Kpis.builder().build();
        }

        Integer seasonId = season.getId();

        // Cost per hectare
        BigDecimal costPerHectare = calculateCostPerHectare(season, seasonId);

        // On-time percentage
        BigDecimal onTimePercent = calculateOnTimePercent(seasonId);

        // Avg yield: actual_yield_kg / plot_area
        BigDecimal avgYieldTonsPerHa = calculateAvgYieldTonsPerHa(season);

        return DashboardOverviewResponse.Kpis.builder()
                .avgYieldTonsPerHa(avgYieldTonsPerHa)
                .costPerHectare(costPerHectare)
                .onTimePercent(onTimePercent)
                .build();
    }

    /**
     * Build expenses summary for a season.
     */
    public DashboardOverviewResponse.Expenses buildExpenses(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (season != null) {
            totalExpense = expenseQueryPort.sumTotalCostBySeasonId(season.getId());
        }
        return DashboardOverviewResponse.Expenses.builder()
                .totalExpense(totalExpense)
                .build();
    }

    public DashboardOverviewResponse.TaskStatusSummary buildTaskStatusSummary(
            org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null || season.getId() == null) {
            return DashboardOverviewResponse.TaskStatusSummary.builder()
                    .totalTasks(0)
                    .pendingTasks(0)
                    .inProgressTasks(0)
                    .completedTasks(0)
                    .overdueTasks(0)
                    .cancelledTasks(0)
                    .byStatus(new LinkedHashMap<>())
                    .build();
        }

        Map<TaskStatus, Long> rawCounts = taskQueryPort.countTaskStatusBySeasonId(season.getId());
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        int total = 0;
        for (TaskStatus status : TaskStatus.values()) {
            int count = rawCounts.getOrDefault(status, 0L).intValue();
            byStatus.put(status.name(), count);
            total += count;
        }

        return DashboardOverviewResponse.TaskStatusSummary.builder()
                .totalTasks(total)
                .pendingTasks(byStatus.getOrDefault(TaskStatus.PENDING.name(), 0))
                .inProgressTasks(byStatus.getOrDefault(TaskStatus.IN_PROGRESS.name(), 0))
                .completedTasks(byStatus.getOrDefault(TaskStatus.DONE.name(), 0))
                .overdueTasks(byStatus.getOrDefault(TaskStatus.OVERDUE.name(), 0))
                .cancelledTasks(byStatus.getOrDefault(TaskStatus.CANCELLED.name(), 0))
                .byStatus(byStatus)
                .build();
    }

    /**
     * Build harvest summary for a season.
     */
    public DashboardOverviewResponse.Harvest buildHarvest(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null) {
            return DashboardOverviewResponse.Harvest.builder()
                    .totalQuantityKg(BigDecimal.ZERO)
                    .totalRevenue(BigDecimal.ZERO)
                    .build();
        }

        Integer seasonId = season.getId();
        BigDecimal totalQty = harvestQueryPort.sumQuantityBySeasonId(seasonId);
        BigDecimal totalRevenue = harvestQueryPort.sumRevenueBySeasonId(seasonId);
        BigDecimal expectedYieldKg = season.getExpectedYieldKg();

        BigDecimal yieldVsPlanPercent = null;
        if (expectedYieldKg != null && expectedYieldKg.compareTo(BigDecimal.ZERO) > 0) {
            yieldVsPlanPercent = totalQty.subtract(expectedYieldKg)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(expectedYieldKg, 1, RoundingMode.HALF_UP);
        }

        return DashboardOverviewResponse.Harvest.builder()
                .totalQuantityKg(totalQty)
                .totalRevenue(totalRevenue)
                .expectedYieldKg(expectedYieldKg)
                .yieldVsPlanPercent(yieldVsPlanPercent)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private BigDecimal calculateCostPerHectare(org.example.QuanLyMuaVu.module.season.entity.Season season, Integer seasonId) {
        BigDecimal totalExpense = expenseQueryPort.sumTotalCostBySeasonId(seasonId);
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = season.getPlot();
        if (plot != null && plot.getArea() != null && plot.getArea().compareTo(BigDecimal.ZERO) > 0) {
            return totalExpense.divide(plot.getArea(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal calculateOnTimePercent(Integer seasonId) {
        long totalCompleted = taskQueryPort.countCompletedBySeasonId(seasonId);
        if (totalCompleted > 0) {
            long onTime = taskQueryPort.countCompletedOnTimeBySeasonId(seasonId);
            return BigDecimal.valueOf(onTime)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCompleted), 1, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal calculateAvgYieldTonsPerHa(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = season.getPlot();
        if (season.getActualYieldKg() != null && plot != null && plot.getArea() != null
                && plot.getArea().compareTo(BigDecimal.ZERO) > 0) {
            return season.getActualYieldKg()
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP) // kg to tons
                    .divide(plot.getArea(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }
}
