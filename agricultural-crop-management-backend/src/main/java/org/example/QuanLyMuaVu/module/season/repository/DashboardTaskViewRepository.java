package org.example.QuanLyMuaVu.module.season.repository;

import java.time.LocalDate;
import java.util.List;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardTaskViewRepository extends JpaRepository<DashboardTaskView, Integer> {

    @Query("SELECT d FROM DashboardTaskView d " +
            "WHERE d.userId = :userId " +
            "AND (:seasonId IS NULL OR d.seasonId = :seasonId) " +
            "AND (d.dueDate = :today OR d.plannedDate = :today) " +
            "ORDER BY d.dueDate ASC")
    Page<DashboardTaskView> findTodayTasks(
            @Param("userId") Long userId,
            @Param("seasonId") Integer seasonId,
            @Param("today") LocalDate today,
            Pageable pageable);

    @Query("SELECT d FROM DashboardTaskView d " +
            "WHERE d.userId = :userId " +
            "AND (:seasonId IS NULL OR d.seasonId = :seasonId) " +
            "AND d.dueDate > :today AND d.dueDate <= :untilDate " +
            "AND d.status NOT IN :excludedStatuses " +
            "ORDER BY d.dueDate ASC")
    List<DashboardTaskView> findUpcomingTasks(
            @Param("userId") Long userId,
            @Param("seasonId") Integer seasonId,
            @Param("today") LocalDate today,
            @Param("untilDate") LocalDate untilDate,
            @Param("excludedStatuses") List<TaskStatus> excludedStatuses);
}
