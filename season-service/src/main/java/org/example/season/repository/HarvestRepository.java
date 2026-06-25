package org.example.season.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.example.season.dto.AdminReportProjections;
import org.example.season.entity.Harvest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Integer> {

    List<Harvest> findByHarvestDateBetween(LocalDate start, LocalDate end);

    List<Harvest> findAllBySeasonId(Integer seasonId);

    List<Harvest> findAllBySeasonIdIn(Iterable<Integer> seasonIds);

    boolean existsBySeasonId(Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumQuantityBySeasonId(@Param("seasonId") Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity * h.unit), 0) FROM Harvest h WHERE h.season.id = :seasonId")
    BigDecimal sumRevenueBySeasonId(@Param("seasonId") Integer seasonId);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.harvestDate BETWEEN :startDate AND :endDate")
    BigDecimal sumQuantityByHarvestDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT h.season.id AS seasonId, COALESCE(SUM(h.quantity), 0) AS totalQuantity " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonHarvestAgg> sumQuantityBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    @Query("SELECT h.season.id AS seasonId, COUNT(h.id) AS harvestCount " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonHarvestCountAgg> countBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    @Query("SELECT h.season.id AS seasonId, " +
            "COALESCE(SUM(h.quantity), 0) AS totalQuantity, " +
            "COALESCE(SUM(h.quantity * h.unit), 0) AS totalRevenue " +
            "FROM Harvest h WHERE h.season.id IN :seasonIds GROUP BY h.season.id")
    List<AdminReportProjections.SeasonRevenueAgg> sumRevenueBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);

    @Query(value = """
            SELECT h.* FROM harvests h
            JOIN seasons s ON h.season_id = s.season_id
            JOIN farm_db.plots p ON s.plot_id = p.plot_id
            JOIN farm_db.farms farm ON p.farm_id = farm.farm_id
            WHERE farm.user_id = :ownerId
            ORDER BY h.created_at DESC, h.harvest_id DESC
            """, nativeQuery = true)
    List<Harvest> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
