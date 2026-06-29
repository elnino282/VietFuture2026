package org.example.adminreporting.repository;

import java.util.List;
import org.example.adminreporting.dto.response.DashboardStatsDTO;
import org.example.adminreporting.dto.response.DashboardStatsDTO.SeasonStatusCount;
import org.example.adminreporting.entity.SeasonSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonSummaryRepository extends JpaRepository<SeasonSummary, Integer> {
    @Query("SELECT new org.example.adminreporting.dto.response.DashboardStatsDTO$SeasonStatusCount(s.status, COUNT(s)) FROM SeasonSummary s GROUP BY s.status")
    List<SeasonStatusCount> countSeasonsByStatus();

    @Query("SELECT new org.example.adminreporting.dto.response.DashboardStatsDTO$RiskySeason(" +
           "s.seasonId, s.seasonName, f.farmName, p.plotName, s.status, " +
           "(SELECT COUNT(i) FROM IncidentSummary i WHERE i.seasonId = s.seasonId AND i.status IN ('OPEN', 'IN_PROGRESS')), " +
           "(SELECT COUNT(t) FROM TaskSummary t WHERE t.seasonId = s.seasonId AND t.status = 'OVERDUE'), " +
           "(SELECT COUNT(a) FROM AlertSummary a WHERE a.seasonId = s.seasonId AND UPPER(a.type) IN ('SEASON_RISK', 'SUSTAINABILITY_WARNING') AND UPPER(a.severity) = 'HIGH' AND UPPER(a.status) NOT IN ('RESOLVED', 'DISMISSED')), " +
           "(SELECT COUNT(a2) FROM AlertSummary a2 WHERE a2.seasonId = s.seasonId AND UPPER(a2.type) IN ('INVENTORY_EXPIRING', 'INVENTORY_EXPIRED') AND UPPER(a2.status) NOT IN ('RESOLVED', 'DISMISSED')), " +
           "0L) " +
           "FROM SeasonSummary s " +
           "JOIN PlotSummary p ON p.plotId = s.plotId " +
           "JOIN FarmSummary f ON f.farmId = p.farmId")
    List<DashboardStatsDTO.RiskySeason> findRiskySeasonsRaw();

    List<SeasonSummary> findByPlotId(Integer plotId);
    Page<SeasonSummary> findByPlotId(Integer plotId, Pageable pageable);

    @Query("SELECT s FROM SeasonSummary s " +
           "JOIN PlotSummary p ON s.plotId = p.plotId " +
           "WHERE (:farmId IS NULL OR p.farmId = :farmId) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:cropId IS NULL OR s.cropId = :cropId) " +
           "AND (:plotId IS NULL OR s.plotId = :plotId)")
    Page<SeasonSummary> findSeasonsWithFilters(
            @Param("farmId") Integer farmId,
            @Param("status") String status,
            @Param("cropId") Integer cropId,
            @Param("plotId") Integer plotId,
            Pageable pageable);
}
