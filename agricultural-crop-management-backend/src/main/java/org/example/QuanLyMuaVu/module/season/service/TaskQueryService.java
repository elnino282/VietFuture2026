package org.example.QuanLyMuaVu.module.season.service;


import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.season.repository.DashboardTaskViewRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class TaskQueryService implements TaskQueryPort {

    DashboardTaskViewRepository dashboardTaskViewRepository;
    TaskRepository taskRepository;

    @Override
    public Page<DashboardTaskView> findTodayTasksByUser(Long userId, Integer seasonId, LocalDate today, Pageable pageable) {
        if (userId == null || today == null) {
            return pageable != null ? Page.empty(pageable) : Page.empty();
        }
        if (pageable == null) {
            return Page.empty();
        }
        return dashboardTaskViewRepository.findTodayTasks(userId, seasonId, today, pageable);
    }

    @Override
    public List<DashboardTaskView> findUpcomingTasksByUser(
            Long userId,
            Integer seasonId,
            LocalDate today,
            LocalDate untilDate,
            List<TaskStatus> excludedStatuses) {
        if (userId == null || today == null || untilDate == null || excludedStatuses == null) {
            return List.of();
        }
        if (untilDate.isBefore(today)) {
            return List.of();
        }
        return dashboardTaskViewRepository.findUpcomingTasks(userId, seasonId, today, untilDate, excludedStatuses);
    }

    @Override
    public long countCompletedBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return 0L;
        }
        return taskRepository.countCompletedBySeasonId(seasonId);
    }

    @Override
    public long countCompletedOnTimeBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return 0L;
        }
        return taskRepository.countCompletedOnTimeBySeasonId(seasonId);
    }

    @Override
    public Map<TaskStatus, Long> countTaskStatusBySeasonId(Integer seasonId) {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        if (seasonId == null) {
            return byStatus;
        }

        for (TaskRepository.TaskStatusCountProjection row : taskRepository.countStatusBySeasonId(seasonId)) {
            if (row == null || row.getStatus() == null) {
                continue;
            }
            byStatus.put(row.getStatus(), row.getTotal() != null ? row.getTotal() : 0L);
        }

        return byStatus;
    }

    @Override
    public Optional<Task> findTaskById(Integer taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        return taskRepository.findById(taskId);
    }

    @Override
    public Optional<Task> findTaskByIdAndSeasonId(Integer taskId, Integer seasonId) {
        if (taskId == null || seasonId == null) {
            return Optional.empty();
        }
        return taskRepository.findByIdAndSeasonId(taskId, seasonId);
    }

    @Override
    public boolean existsTaskByIdAndSeasonId(Integer taskId, Integer seasonId) {
        if (taskId == null || seasonId == null) {
            return false;
        }
        return taskRepository.existsByIdAndSeasonId(taskId, seasonId);
    }

    @Override
    public long countTasksBySeasonIdAndStatusNot(Integer seasonId, TaskStatus status) {
        if (seasonId == null || status == null) {
            return 0L;
        }
        return taskRepository.countBySeason_IdAndStatusNot(seasonId, status);
    }

    @Override
    public List<Task> findTasksBySeasonIdAndStatusNot(Integer seasonId, TaskStatus status) {
        if (seasonId == null || status == null) {
            return List.of();
        }
        return taskRepository.findBySeason_IdAndStatusNot(seasonId, status);
    }

    @Override
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }
}
