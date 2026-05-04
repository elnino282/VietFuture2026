package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Summary summary;
    private List<UserRoleCount> userRoleCounts;
    private List<UserStatusCount> userStatusCounts;
    private List<SeasonStatusCount> seasonStatusCounts;
    private List<RiskySeason> riskySeasons;
    private List<InventoryHealth> inventoryHealth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long totalUsers;
        private Long totalFarms;
        private Long totalPlots;
        private Long totalSeasons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleCount {
        private String role;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusCount {
        private String status;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonStatusCount {
        private String status;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskySeason {
        private Integer seasonId;
        private String seasonName;
        private String farmName;
        private String plotName;
        private String status;
        private Long incidentCount;
        private Long overdueTaskCount;
        private Long riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryHealth {
        private Integer farmId;
        private String farmName;
        private Long expiredCount;
        private Long expiringSoonCount;
        private Long totalAtRisk;
    }
}
