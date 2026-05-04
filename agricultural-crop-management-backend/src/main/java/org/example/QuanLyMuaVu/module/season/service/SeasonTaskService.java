package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.TaskCompletedEvent;
import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateTaskStatusRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.season.repository.SeasonEmployeeRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonTaskService {

    TaskRepository taskRepository;
    SeasonRepository seasonRepository;
    IdentityQueryPort identityQueryPort;
    SeasonEmployeeRepository seasonEmployeeRepository;
    FarmAccessPort farmAccessService;
    LaborManagementService laborManagementService;
    DomainEventPublisher domainEventPublisher;

    public PageResponse<TaskResponse> listTasksForSeason(
            Integer seasonId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);

        TaskStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = TaskStatus.fromCode(status);
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        final TaskStatus statusFilterFinal = statusFilter;
        final LocalDate fromFilter = from;
        final LocalDate toFilter = to;

        List<Task> all = taskRepository.findAllBySeason_Id(season.getId());

        List<TaskResponse> items = all.stream()
                .filter(task -> statusFilterFinal == null || statusFilterFinal.equals(task.getStatus()))
                .filter(task -> {
                    if (fromFilter == null && toFilter == null) {
                        return true;
                    }
                    LocalDate date = task.getDueDate() != null ? task.getDueDate() : task.getPlannedDate();
                    if (date == null) {
                        return false;
                    }
                    boolean afterFrom = fromFilter == null || !date.isBefore(fromFilter);
                    boolean beforeTo = toFilter == null || !date.isAfter(toFilter);
                    return afterFrom && beforeTo;
                })
                .sorted((t1, t2) -> Integer.compare(
                        t2.getId() != null ? t2.getId() : 0,
                        t1.getId() != null ? t1.getId() : 0))
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<TaskResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TaskResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    public TaskResponse createTask(Integer seasonId, CreateTaskRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForTasks(season, true);

        LocalDate planned = request.getPlannedDate();
        LocalDate due = request.getDueDate();
        validateTaskDatesWithinSeason(season, planned, due, null, null);

        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        org.example.QuanLyMuaVu.module.identity.entity.User assignee = resolveTaskAssignee(season, request.getAssigneeUserId(), currentUser);

        Task task = Task.builder()
                .user(assignee)
                .season(season)
                .title(request.getTitle())
                .description(request.getDescription())
                .plannedDate(planned)
                .dueDate(due)
                .status(TaskStatus.PENDING)
                .build();

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse getTask(Integer id) {
        Task task = getTaskForCurrentFarmer(id);
        return toResponse(task);
    }

    public TaskResponse updateTask(Integer id, UpdateTaskRequest request) {
        Task task = getTaskForCurrentFarmer(id);
        ensureSeasonOpenForTasks(task.getSeason(), false);

        if (isTerminalStatus(task.getStatus())) {
            throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_TASK);
        }

        LocalDate planned = request.getPlannedDate();
        LocalDate due = request.getDueDate();
        validateTaskDatesWithinSeason(task.getSeason(), planned, due, task.getActualStartDate(),
                task.getActualEndDate());

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPlannedDate(planned);
        task.setDueDate(due);
        if (request.getAssigneeUserId() != null) {
            org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
            task.setUser(resolveTaskAssignee(task.getSeason(), request.getAssigneeUserId(), currentUser));
        }

        Task saved = taskRepository.save(task);
        laborManagementService.syncPayrollForTask(saved);
        return toResponse(saved);
    }

    public TaskResponse updateTaskStatus(Integer id, UpdateTaskStatusRequest request) {
        Task task = getTaskForCurrentFarmer(id);
        ensureSeasonOpenForTasks(task.getSeason(), false);

        TaskStatus targetStatus;
        try {
            targetStatus = TaskStatus.fromCode(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        TaskStatus currentStatus = task.getStatus();
        if (!isValidStatusTransition(currentStatus, targetStatus)) {
            throw new AppException(ErrorCode.INVALID_TASK_STATUS_TRANSITION);
        }

        LocalDate actualStart = request.getActualStartDate();
        LocalDate actualEnd = request.getActualEndDate();

        if (targetStatus == TaskStatus.DONE) {
            if (actualStart == null || actualEnd == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (actualEnd.isBefore(actualStart)) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        validateTaskDatesWithinSeason(task.getSeason(), task.getPlannedDate(), task.getDueDate(), actualStart,
                actualEnd);

        task.setStatus(targetStatus);
        if (actualStart != null) {
            task.setActualStartDate(actualStart);
        }
        if (actualEnd != null) {
            task.setActualEndDate(actualEnd);
        }
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        Task saved = taskRepository.save(task);
        laborManagementService.syncPayrollForTask(saved);
        if (targetStatus == TaskStatus.DONE && currentStatus != TaskStatus.DONE) {
            domainEventPublisher.publish(new TaskCompletedEvent(saved, currentStatus));
        }
        return toResponse(saved);
    }

    public void deleteTask(Integer id) {
        Task task = getTaskForCurrentFarmer(id);
        ensureSeasonOpenForTasks(task.getSeason(), false);

        if (isTerminalStatus(task.getStatus())) {
            throw new AppException(ErrorCode.INVALID_TASK_STATUS_TRANSITION);
        }

        taskRepository.delete(task);
    }

    private boolean isValidStatusTransition(TaskStatus currentStatus, TaskStatus targetStatus) {
        if (currentStatus == null) {
            return targetStatus == TaskStatus.PENDING;
        }
        if (currentStatus == targetStatus) {
            return true;
        }

        return switch (currentStatus) {
            case PENDING ->
                EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED, TaskStatus.OVERDUE)
                        .contains(targetStatus);
            case IN_PROGRESS ->
                EnumSet.of(TaskStatus.DONE, TaskStatus.CANCELLED, TaskStatus.OVERDUE).contains(targetStatus);
            case DONE, CANCELLED, OVERDUE -> false;
        };
    }

    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.DONE || status == TaskStatus.CANCELLED;
    }

    private void ensureSeasonOpenForTasks(Season season, boolean forCreate) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            if (forCreate) {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_ADD_TASK);
            } else {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_TASK);
            }
        }
    }

    private void validateTaskDatesWithinSeason(
            Season season,
            LocalDate plannedDate,
            LocalDate dueDate,
            LocalDate actualStart,
            LocalDate actualEnd) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate();
        if (end == null) {
            LocalDate plannedHarvest = season.getPlannedHarvestDate();
            if (plannedHarvest != null) {
                boolean isActiveOverdue = season.getStatus() == SeasonStatus.ACTIVE
                        && plannedHarvest.isBefore(LocalDate.now());
                if (!isActiveOverdue) {
                    end = plannedHarvest;
                }
            }
        }

        if (plannedDate != null) {
            if (plannedDate.isBefore(start) || (end != null && plannedDate.isAfter(end))) {
                throw new AppException(ErrorCode.INVALID_SEASON_DATES);
            }
        }
        if (dueDate != null) {
            if (dueDate.isBefore(start) || (end != null && dueDate.isAfter(end))) {
                throw new AppException(ErrorCode.INVALID_SEASON_DATES);
            }
        }
        if (actualStart != null) {
            if (actualStart.isBefore(start) || (end != null && actualStart.isAfter(end))) {
                throw new AppException(ErrorCode.INVALID_SEASON_DATES);
            }
        }
        if (actualEnd != null) {
            if (actualEnd.isBefore(start) || (end != null && actualEnd.isAfter(end))) {
                throw new AppException(ErrorCode.INVALID_SEASON_DATES);
            }
        }
    }

    private Task getTaskForCurrentFarmer(Integer id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        Season season = task.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }

        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return task;
    }

    private Season getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser() {
        return farmAccessService.getCurrentUser();
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User resolveTaskAssignee(Season season, Long assigneeUserId, org.example.QuanLyMuaVu.module.identity.entity.User fallbackAssignee) {
        if (assigneeUserId == null) {
            return fallbackAssignee;
        }

        org.example.QuanLyMuaVu.module.identity.entity.User assignee = identityQueryPort.findUserById(assigneeUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (assignee.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        boolean hasEmployeeRole = identityQueryPort
                .existsUserByIdAndRoleCode(assignee.getId(), PredefinedRole.EMPLOYEE_ROLE);
        if (!hasEmployeeRole) {
            throw new AppException(ErrorCode.EMPLOYEE_ROLE_REQUIRED);
        }

        boolean isSeasonEmployee = seasonEmployeeRepository.existsBySeason_IdAndEmployee_Id(season.getId(), assignee.getId());
        if (!isSeasonEmployee) {
            throw new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND);
        }

        return assignee;
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .notes(task.getNotes())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
