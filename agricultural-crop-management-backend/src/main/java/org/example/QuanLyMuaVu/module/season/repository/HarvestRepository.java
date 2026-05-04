package org.example.QuanLyMuaVu.module.season.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportProjections;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Integer> {

    List<Harvest> findByHarvestDateBetween(LocalDate start, LocalDate end);

    List<Harvest> findAllBySeason_Id(Integer seasonId);

    List<Harvest> findAllBySeason_IdIn(Iterable<Integer> seasonIds);

    boolean existsBySeason_Id(Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumQuantityBySeasonId(@Param("seasonId") Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity * h.unit), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumRevenueBySeasonId(@Param("seasonId") Integer seasonId);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN AGGREGATION METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sum harvest quantities for date range (admin dashboard).
     */
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.harvestDate BETWEEN :startDate AND :endDate")
    BigDecimal sumQuantityByHarvestDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum harvest quantity grouped by season ID.
     */
    @Query("SELECT h.season.id AS seasonId, COALESCE(SUM(h.quantity), 0) AS totalQuantity " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonHarvestAgg> sumQuantityBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    /**
     * Count harvest batches grouped by season ID.
     */
    @Query("SELECT h.season.id AS seasonId, COUNT(h.id) AS harvestCount " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonHarvestCountAgg> countBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    /**
     * Sum revenue (quantity * unit) grouped by season ID.
     */
    @Query("SELECT h.season.id AS seasonId, " +
            "COALESCE(SUM(h.quantity), 0) AS totalQuantity, " +
            "COALESCE(SUM(h.quantity * h.unit), 0) AS totalRevenue " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonRevenueAgg> sumRevenueBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);
}
