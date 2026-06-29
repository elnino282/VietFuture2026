package org.example.adminreporting.repository;

import org.example.adminreporting.entity.TaskSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskSummaryRepository extends JpaRepository<TaskSummary, Integer> {
    long countBySeasonIdAndStatus(Integer seasonId, String status);

    @Query("SELECT COUNT(t) FROM TaskSummary t WHERE t.seasonId = :seasonId AND UPPER(t.status) != 'DONE'")
    long countPendingTasks(@Param("seasonId") Integer seasonId);

    Page<TaskSummary> findBySeasonId(Integer seasonId, Pageable pageable);
    Page<TaskSummary> findByStatus(String status, Pageable pageable);
    Page<TaskSummary> findBySeasonIdAndStatus(Integer seasonId, String status, Pageable pageable);

    @Query("SELECT t FROM TaskSummary t " +
           "JOIN SeasonSummary s ON t.seasonId = s.seasonId " +
           "JOIN PlotSummary p ON s.plotId = p.plotId " +
           "WHERE (:farmId IS NULL OR p.farmId = :farmId) " +
           "AND (:cropId IS NULL OR s.cropId = :cropId) " +
           "AND (:seasonId IS NULL OR t.seasonId = :seasonId) " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<TaskSummary> findTasksWithFilters(
            @Param("farmId") Integer farmId,
            @Param("cropId") Integer cropId,
            @Param("seasonId") Integer seasonId,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM TaskSummary t " +
           "JOIN SeasonSummary s ON t.seasonId = s.seasonId " +
           "WHERE (:year IS NULL OR YEAR(s.startDate) = :year) " +
           "AND (:status IS NULL OR UPPER(t.status) = UPPER(:status))")
    long countTasksByYearAndStatus(@Param("year") Integer year, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM TaskSummary t " +
           "JOIN SeasonSummary s ON t.seasonId = s.seasonId " +
           "WHERE (:year IS NULL OR YEAR(s.startDate) = :year)")
    long countTasksByYear(@Param("year") Integer year);
}
