package org.example.QuanLyMuaVu.Service.Dashboard;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardKpiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardKpiServiceTest {

    @Mock
    private TaskQueryPort taskQueryPort;

    @Mock
    private ExpenseQueryPort expenseQueryPort;

    @Mock
    private HarvestQueryPort harvestQueryPort;

    @InjectMocks
    private DashboardKpiService dashboardKpiService;

    @Test
    @DisplayName("buildKpis computes cost/ha, on-time percent and avg yield")
    void buildKpis_ComputesExpectedMetrics() {
        Season season = Season.builder()
                .id(10)
                .actualYieldKg(BigDecimal.valueOf(25000))
                .plot(Plot.builder().area(BigDecimal.TEN).build())
                .build();

        when(expenseQueryPort.sumTotalCostBySeasonId(10)).thenReturn(BigDecimal.valueOf(1000));
        when(taskQueryPort.countCompletedBySeasonId(10)).thenReturn(8L);
        when(taskQueryPort.countCompletedOnTimeBySeasonId(10)).thenReturn(6L);

        DashboardOverviewResponse.Kpis kpis = dashboardKpiService.buildKpis(season);

        assertNotNull(kpis);
        assertEquals(new BigDecimal("100.00"), kpis.getCostPerHectare());
        assertEquals(new BigDecimal("75.0"), kpis.getOnTimePercent());
        assertEquals(new BigDecimal("2.50"), kpis.getAvgYieldTonsPerHa());
    }

    @Test
    @DisplayName("buildExpenses returns zero for null season")
    void buildExpenses_WithNullSeason_ReturnsZero() {
        DashboardOverviewResponse.Expenses expenses = dashboardKpiService.buildExpenses(null);

        assertNotNull(expenses);
        assertEquals(BigDecimal.ZERO, expenses.getTotalExpense());
    }

    @Test
    @DisplayName("buildHarvest computes quantity/revenue and yield-vs-plan")
    void buildHarvest_ComputesYieldVsPlan() {
        Season season = Season.builder()
                .id(11)
                .expectedYieldKg(BigDecimal.valueOf(2000))
                .build();

        when(harvestQueryPort.sumQuantityBySeasonId(11)).thenReturn(BigDecimal.valueOf(1500));
        when(harvestQueryPort.sumRevenueBySeasonId(11)).thenReturn(BigDecimal.valueOf(3000));

        DashboardOverviewResponse.Harvest harvest = dashboardKpiService.buildHarvest(season);

        assertNotNull(harvest);
        assertEquals(BigDecimal.valueOf(1500), harvest.getTotalQuantityKg());
        assertEquals(BigDecimal.valueOf(3000), harvest.getTotalRevenue());
        assertEquals(new BigDecimal("-25.0"), harvest.getYieldVsPlanPercent());
    }

    @Test
    @DisplayName("buildKpis returns empty metrics when season is null")
    void buildKpis_WithNullSeason_ReturnsEmptyObject() {
        DashboardOverviewResponse.Kpis kpis = dashboardKpiService.buildKpis(null);

        assertNotNull(kpis);
        assertTrue(kpis.getCostPerHectare() == null && kpis.getOnTimePercent() == null && kpis.getAvgYieldTonsPerHa() == null);
    }

    @Test
    @DisplayName("buildTaskStatusSummary aggregates status counts")
    void buildTaskStatusSummary_AggregatesStatuses() {
        Season season = Season.builder().id(22).build();
        when(taskQueryPort.countTaskStatusBySeasonId(22)).thenReturn(Map.of(
                TaskStatus.PENDING, 3L,
                TaskStatus.IN_PROGRESS, 2L,
                TaskStatus.DONE, 5L,
                TaskStatus.OVERDUE, 1L,
                TaskStatus.CANCELLED, 1L));

        DashboardOverviewResponse.TaskStatusSummary summary = dashboardKpiService.buildTaskStatusSummary(season);

        assertNotNull(summary);
        assertEquals(12, summary.getTotalTasks());
        assertEquals(3, summary.getPendingTasks());
        assertEquals(2, summary.getInProgressTasks());
        assertEquals(5, summary.getCompletedTasks());
        assertEquals(1, summary.getOverdueTasks());
        assertEquals(1, summary.getCancelledTasks());
        assertEquals(5, summary.getByStatus().size());
    }
}
