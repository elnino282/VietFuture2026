package org.example.QuanLyMuaVu.module.season.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Season> {

        List<Season> findByPlot(org.example.QuanLyMuaVu.module.farm.entity.Plot plot);

        List<Season> findBySeasonNameContainingIgnoreCase(String seasonName);

        boolean existsBySeasonNameIgnoreCase(String seasonName);

        boolean existsByPlot_Id(Integer plotId);

        boolean existsByPlot_IdAndStatusIn(Integer plotId, Iterable<SeasonStatus> statuses);

        List<Season> findAllByPlot_Id(Integer plotId);

        List<Season> findAllByPlot_IdOrderByStartDateAsc(Integer plotId);

        List<Season> findAllByPlot_IdOrderByStartDateDesc(Integer plotId);

        List<Season> findAllByPlot_User_Id(Long userId);

        List<Season> findAllByPlot_Farm_IdIn(Iterable<Integer> farmIds);

        List<Season> findAllByPlot_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Integer plotId,
                        LocalDate endDate,
                        LocalDate startDate);

        /**
         * Find all seasons for a given plot with status in the specified set.
         * Used by ActiveSeasonValidator to check for overlapping seasons.
         */
        List<Season> findByPlotAndStatusIn(org.example.QuanLyMuaVu.module.farm.entity.Plot plot, Iterable<SeasonStatus> statuses);

        /**
         * Find season by ID only if the farm owner matches.
         * Used for ownership verification: seasons.plot_id -> plots.farm_id ->
         * farms.owner_id
         * 
         * @param seasonId the season ID
         * @param ownerId  the expected farm owner's user ID
         * @return Optional containing the season if found and owned
         */
        @Query("SELECT s FROM Season s WHERE s.id = :seasonId AND s.plot.farm.user.id = :ownerId")
        Optional<Season> findByIdAndFarmUserId(@Param("seasonId") Integer seasonId, @Param("ownerId") Long ownerId);

        /**
         * Find all seasons for farms owned by the specified user.
         * 
         * @param ownerId the farm owner's user ID
         * @return list of seasons for farms owned by the user
         */
        @Query("SELECT s FROM Season s WHERE s.plot.farm.user.id = :ownerId")
        List<Season> findAllByFarmUserId(@Param("ownerId") Long ownerId);

        /**
         * Check if a season exists and is owned by the specified user (via plot ->
         * farm).
         * 
         * @param seasonId the season ID
         * @param ownerId  the expected farm owner's user ID
         * @return true if season exists and farm is owned by user
         */
        @Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.id = :seasonId AND s.plot.farm.user.id = :ownerId")
        boolean existsByIdAndFarmUserId(@Param("seasonId") Integer seasonId, @Param("ownerId") Long ownerId);

        /**
         * Count seasons by status for a specific farm owner.
         * Used for dashboard season status breakdown.
         */
        @Query("SELECT COUNT(s) FROM Season s WHERE s.status = :status AND s.plot.farm.user.id = :ownerId")
        long countByStatusAndFarmUserId(@Param("status") SeasonStatus status, @Param("ownerId") Long ownerId);

        /**
         * Find the most recent ACTIVE season for an owner, ordered by start_date desc.
         * Used as default season for dashboard.
         */
        @Query("SELECT s FROM Season s WHERE s.status = 'ACTIVE' AND s.plot.farm.user.id = :ownerId ORDER BY s.startDate DESC")
        List<Season> findActiveSeasonsByUserIdOrderByStartDateDesc(@Param("ownerId") Long ownerId);

        /**
         * BR8/BR12: Check if a season name already exists within a plot
         * (case-insensitive).
         * Used to enforce uniqueness constraint for season names per plot.
         * Excludes archived seasons and the season being updated (excludeId).
         *
         * @param plotId     the plot ID
         * @param seasonName the season name to check
         * @param excludeId  the season ID to exclude (for update operations), use -1
         *                   for create
         * @return true if a matching season exists
         */
        @Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.plot.id = :plotId " +
                        "AND LOWER(s.seasonName) = LOWER(:seasonName) " +
                        "AND s.status <> 'ARCHIVED' " +
                        "AND (:excludeId IS NULL OR s.id <> :excludeId)")
        boolean existsByPlotIdAndSeasonNameIgnoreCaseExcluding(
                        @Param("plotId") Integer plotId,
                        @Param("seasonName") String seasonName,
                        @Param("excludeId") Integer excludeId);

        /**
         * BR17: Search seasons by keyword (matches season name, plot name, or crop
         * name).
         * Used for the Text_change() search functionality.
         *
         * @param keyword the search keyword
         * @param ownerId the farm owner's user ID
         * @return list of matching seasons
         */
        @Query("SELECT s FROM Season s WHERE s.plot.farm.user.id = :ownerId " +
                        "AND (LOWER(s.seasonName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.plot.plotName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.crop.cropName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "ORDER BY s.startDate DESC")
        List<Season> searchByKeywordAndUserId(@Param("keyword") String keyword, @Param("ownerId") Long ownerId);

        // ═══════════════════════════════════════════════════════════════
        // ADMIN QUERY METHODS (global - no owner filter)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Count seasons by status globally (admin dashboard).
         */
        long countByStatus(SeasonStatus status);

        /**
         * Check if any seasons reference a specific variety.
         * Used for variety deletion validation.
         */
        boolean existsByVariety_Id(Integer varietyId);

        /**
         * Find seasons by date range with optional filters.
         * Uses date range for index optimization (no YEAR() function).
         */
        @Query("SELECT s FROM Season s " +
                        "WHERE (:from IS NULL OR s.startDate >= :from) " +
                        "AND (:to IS NULL OR s.startDate < :to) " +
                        "AND (:cropId IS NULL OR s.crop.id = :cropId) " +
                        "AND (:farmId IS NULL OR s.plot.farm.id = :farmId) " +
                        "AND (:plotId IS NULL OR s.plot.id = :plotId) " +
                        "AND (:varietyId IS NULL OR s.variety.id = :varietyId)")
        List<Season> findByFilters(@Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        @Param("cropId") Integer cropId,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("varietyId") Integer varietyId);
}
