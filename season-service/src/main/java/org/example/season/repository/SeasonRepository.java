package org.example.season.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.season.entity.Season;
import org.example.season.enums.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Season> {

        List<Season> findBySeasonNameContainingIgnoreCase(String seasonName);

        boolean existsBySeasonNameIgnoreCase(String seasonName);

        @Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.plotId = :plotId")
        boolean existsByPlotId(@Param("plotId") Integer plotId);

        @Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.plotId = :plotId AND s.status IN :statuses")
        boolean existsByPlotIdAndStatusIn(@Param("plotId") Integer plotId, @Param("statuses") Iterable<SeasonStatus> statuses);

        List<Season> findAllByPlotId(Integer plotId);

        List<Season> findAllByPlotIdOrderByStartDateAsc(Integer plotId);

        List<Season> findAllByPlotIdOrderByStartDateDesc(Integer plotId);

        @Query(value = "SELECT s.* FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id WHERE p.user_id = :userId", nativeQuery = true)
        List<Season> findAllByPlotUserId(@Param("userId") Long userId);

        @Query(value = "SELECT s.* FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id WHERE p.farm_id IN :farmIds", nativeQuery = true)
        List<Season> findAllByPlotFarmIdIn(@Param("farmIds") Iterable<Integer> farmIds);

        List<Season> findAllByPlotIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Integer plotId,
                        LocalDate endDate,
                        LocalDate startDate);

        @Query("SELECT s FROM Season s WHERE s.plotId = :plotId AND s.status IN :statuses")
        List<Season> findByPlotIdAndStatusIn(@Param("plotId") Integer plotId, @Param("statuses") Iterable<SeasonStatus> statuses);

        @Query(value = "SELECT s.* FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id JOIN farm_db.farms f ON p.farm_id = f.farm_id WHERE s.season_id = :seasonId AND f.user_id = :ownerId", nativeQuery = true)
        Optional<Season> findByIdAndFarmUserId(@Param("seasonId") Integer seasonId, @Param("ownerId") Long ownerId);

        @Query(value = "SELECT s.* FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id JOIN farm_db.farms f ON p.farm_id = f.farm_id WHERE f.user_id = :ownerId", nativeQuery = true)
        List<Season> findAllByFarmUserId(@Param("ownerId") Long ownerId);

        @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id JOIN farm_db.farms f ON p.farm_id = f.farm_id WHERE s.season_id = :seasonId AND f.user_id = :ownerId", nativeQuery = true)
        boolean existsByIdAndFarmUserId(@Param("seasonId") Integer seasonId, @Param("ownerId") Long ownerId);

        @Query(value = "SELECT COUNT(*) FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id JOIN farm_db.farms f ON p.farm_id = f.farm_id WHERE s.status = :status AND f.user_id = :ownerId", nativeQuery = true)
        long countByStatusAndFarmUserId(@Param("status") String status, @Param("ownerId") Long ownerId);

        @Query(value = "SELECT s.* FROM seasons s JOIN farm_db.plots p ON s.plot_id = p.plot_id JOIN farm_db.farms f ON p.farm_id = f.farm_id WHERE s.status = 'ACTIVE' AND f.user_id = :ownerId ORDER BY s.start_date DESC", nativeQuery = true)
        List<Season> findActiveSeasonsByUserIdOrderByStartDateDesc(@Param("ownerId") Long ownerId);

        @Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.plotId = :plotId " +
                        "AND LOWER(s.seasonName) = LOWER(:seasonName) " +
                        "AND s.status <> 'ARCHIVED' " +
                        "AND (:excludeId IS NULL OR s.id <> :excludeId)")
        boolean existsByPlotIdAndSeasonNameIgnoreCaseExcluding(
                        @Param("plotId") Integer plotId,
                        @Param("seasonName") String seasonName,
                        @Param("excludeId") Integer excludeId);

        @Query(value = "SELECT s.* FROM seasons s " +
                        "JOIN farm_db.plots p ON s.plot_id = p.plot_id " +
                        "JOIN farm_db.farms f ON p.farm_id = f.farm_id " +
                        "JOIN crop_catalog_db.crops c ON s.crop_id = c.crop_id " +
                        "WHERE f.user_id = :ownerId " +
                        "AND (LOWER(s.season_name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(p.plot_name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(c.crop_name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "ORDER BY s.start_date DESC", nativeQuery = true)
        List<Season> searchByKeywordAndUserId(@Param("keyword") String keyword, @Param("ownerId") Long ownerId);

        long countByStatus(SeasonStatus status);

        boolean existsByVarietyId(Integer varietyId);

        @Query(value = "SELECT s.* FROM seasons s " +
                        "JOIN farm_db.plots p ON s.plot_id = p.plot_id " +
                        "JOIN farm_db.farms f ON p.farm_id = f.farm_id " +
                        "WHERE (:from IS NULL OR s.start_date >= :from) " +
                        "AND (:to IS NULL OR s.start_date < :to) " +
                        "AND (:cropId IS NULL OR s.crop_id = :cropId) " +
                        "AND (:farmId IS NULL OR f.farm_id = :farmId) " +
                        "AND (:plotId IS NULL OR s.plot_id = :plotId) " +
                        "AND (:varietyId IS NULL OR s.variety_id = :varietyId)", nativeQuery = true)
        List<Season> findByFilters(@Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        @Param("cropId") Integer cropId,
                        @Param("farmId") Integer farmId,
                        @Param("plotId") Integer plotId,
                        @Param("varietyId") Integer varietyId);
}
