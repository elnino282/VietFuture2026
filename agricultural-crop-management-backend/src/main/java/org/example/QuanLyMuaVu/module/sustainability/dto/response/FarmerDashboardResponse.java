package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerDashboardResponse {
    private SummaryMetrics summary;
    private RecentActivity recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryMetrics {
        private Long activeSeasonsCount;
        private Long tasksDueSoonCount;
        private BigDecimal totalExpensesThisMonth;
        private BigDecimal totalHarvestLast30Days;
        private Long openIncidentsCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private java.util.List<TaskSummary> latestTasks;
        private java.util.List<ExpenseSummary> latestExpenses;
        private java.util.List<FieldLogSummary> latestFieldLogs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummary {
        private Integer taskId;
        private String title;
        private String status;
        private java.time.LocalDate dueDate;
        private String seasonName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSummary {
        private Integer expenseId;
        private String itemName;
        private BigDecimal totalCost;
        private java.time.LocalDate expenseDate;
        private String seasonName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldLogSummary {
        private Integer fieldLogId;
        private String logType;
        private java.time.LocalDate logDate;
        private String notes;
        private String seasonName;
    }
}
