package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private SummaryMetrics summary;
    private LatestItems latestItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryMetrics {
        private Long totalUsers;
        private Long totalFarms;
        private Long activeSeasonsCount;
        private Long openIncidentsCount;
        private BigDecimal expensesThisMonth;
        private BigDecimal harvestThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestItems {
        private List<IncidentSummary> latestIncidents;
        private List<SeasonSummary> latestSeasons;
        private List<MovementSummary> latestMovements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentSummary {
        private Integer id;
        private String incidentType;
        private String severity;
        private String status;
        private LocalDateTime createdAt;
        private String seasonName;
        private String farmName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonSummary {
        private Integer seasonId;
        private String seasonName;
        private String status;
        private String cropName;
        private String plotName;
        private String farmName;
        private LocalDate startDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementSummary {
        private Integer id;
        private String movementType;
        private BigDecimal quantity;
        private LocalDateTime movementDate;
        private String supplyItemName;
        private String warehouseName;
    }
}
