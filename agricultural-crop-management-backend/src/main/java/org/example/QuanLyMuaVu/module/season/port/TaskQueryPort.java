package org.example.QuanLyMuaVu.module.season.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskQueryPort {

    Page<DashboardTaskView> findTodayTasksByUser(Long userId, Integer seasonId, LocalDate today, Pageable pageable);

    List<DashboardTaskView> findUpcomingTasksByUser(
            Long userId,
            Integer seasonId,
            LocalDate today,
            LocalDate untilDate,
            List<TaskStatus> excludedStatuses);

    long countCompletedBySeasonId(Integer seasonId);

    long countCompletedOnTimeBySeasonId(Integer seasonId);

    Map<TaskStatus, Long> countTaskStatusBySeasonId(Integer seasonId);

    Optional<Task> findTaskById(Integer taskId);

    Optional<Task> findTaskByIdAndSeasonId(Integer taskId, Integer seasonId);

    boolean existsTaskByIdAndSeasonId(Integer taskId, Integer seasonId);

    long countTasksBySeasonIdAndStatusNot(Integer seasonId, TaskStatus status);

    List<Task> findTasksBySeasonIdAndStatusNot(Integer seasonId, TaskStatus status);

    List<Task> findAllTasks();
}
