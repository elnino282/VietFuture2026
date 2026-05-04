package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
import org.example.QuanLyMuaVu.module.season.dto.request.StartTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.TaskDoneRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonMinimalResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.season.repository.SeasonEmployeeRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Tasks Workspace - user-scoped task management.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TaskWorkspaceService {

    TaskRepository taskRepository;
    SeasonRepository seasonRepository;
    IdentityQueryPort identityQueryPort;
    SeasonEmployeeRepository seasonEmployeeRepository;
    FarmAccessPort farmAccessService;
    LaborManagementService laborManagementService;
    DomainEventPublisher domainEventPublisher;

    /**
     * Create a new task for the current user.
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();

        // Validate dates
        validateTaskDates(request.getPlannedDate(), request.getDueDate(), null, null);

        // Validate season ownership if seasonId is provided
        Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(season);
            ensureSeasonOpenForWorkspaceTask(season, true);
        }
        org.example.QuanLyMuaVu.module.identity.entity.User assignee = resolveWorkspaceAssignee(request.getAssigneeUserId(), currentUser, season);

        // Build task
        Task task = Task.builder()
                .user(assignee)
                .season(season)
                .title(request.getTitle())
                .description(request.getDescription())
                .plannedDate(request.getPlannedDate())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // Check if task should be OVERDUE immediately
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            task.setStatus(TaskStatus.OVERDUE);
        }

        task = taskRepository.save(task);
        log.info("Created task {} for user {}", task.getId(), currentUser.getId());

        return mapToResponse(task);
    }

    /**
     * List tasks for current user with filters and pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(
            TaskStatus status,
            Integer seasonId,
            String searchQuery,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();

        // Auto-update overdue tasks before listing
        updateOverdueTasksForUser(currentUser);

        // Build pagination
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20, sort);

        // Execute query with filters
        Page<Task> taskPage = taskRepository.findByUserWithFilters(
                currentUser,
                status,
                seasonId,
                searchQuery,
                pageable);

        List<TaskResponse> responses = taskPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PageResponse<TaskResponse> response = new PageResponse<>();
        response.setItems(responses);
        response.setPage(taskPage.getNumber());
        response.setSize(taskPage.getSize());
        response.setTotalElements(taskPage.getTotalElements());
        response.setTotalPages(taskPage.getTotalPages());
        return response;
    }

    /**
     * Get task by ID (must belong to current user).
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Integer taskId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        return mapToResponse(task);
    }

    /**
     * Update task details.
     */
    @Transactional
    public TaskResponse updateTask(Integer taskId, UpdateTaskRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureSeasonOpenForWorkspaceTask(task.getSeason(), false);

        // Cannot update CANCELLED tasks
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        // Validate dates
        validateTaskDates(
                request.getPlannedDate(),
                request.getDueDate(),
                task.getActualStartDate(),
                task.getActualEndDate());

        // Validate season ownership if changed
        if (request.getSeasonId() != null && !request.getSeasonId().equals(
                task.getSeason() != null ? task.getSeason().getId() : null)) {
            Season newSeason = seasonRepository.findById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(newSeason);
            ensureSeasonOpenForWorkspaceTask(newSeason, false);
            task.setSeason(newSeason);
        } else if (request.getSeasonId() == null) {
            task.setSeason(null);
        }

        // Update fields
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPlannedDate(request.getPlannedDate());
        task.setDueDate(request.getDueDate());
        task.setNotes(request.getNotes());
        if (request.getAssigneeUserId() != null) {
            task.setUser(resolveWorkspaceAssignee(request.getAssigneeUserId(), currentUser, task.getSeason()));
        }

        // Recheck overdue status
        if (task.getStatus() == TaskStatus.OVERDUE) {
            // If due date is now in future, revert to IN_PROGRESS or PENDING
            if (task.getDueDate() == null || !task.getDueDate().isBefore(LocalDate.now())) {
                if (task.getActualStartDate() != null) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                } else {
                    task.setStatus(TaskStatus.PENDING);
                }
            }
        } else if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED) {
            // Check if it should become overdue
            if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                task.setStatus(TaskStatus.OVERDUE);
            }
        }

        task = taskRepository.save(task);
        laborManagementService.syncPayrollForTask(task);
        log.info("Updated task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Start a task (set actualStartDate and status to IN_PROGRESS).
     */
    @Transactional
    public TaskResponse startTask(Integer taskId, StartTaskRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureSeasonOpenForWorkspaceTask(task.getSeason(), false);

        // Cannot start DONE or CANCELLED tasks
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        LocalDate startDate = request.getActualStartDate() != null ? request.getActualStartDate() : LocalDate.now();
        task.setActualStartDate(startDate);
        task.setStatus(TaskStatus.IN_PROGRESS);

        task = taskRepository.save(task);
        laborManagementService.syncPayrollForTask(task);
        log.info("Started task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Mark task as done (set actualEndDate and status to DONE).
     */
    @Transactional
    public TaskResponse doneTask(Integer taskId, TaskDoneRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureSeasonOpenForWorkspaceTask(task.getSeason(), false);

        // Cannot mark CANCELLED tasks as done
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        LocalDate endDate = request.getActualEndDate() != null ? request.getActualEndDate() : LocalDate.now();
        TaskStatus previousStatus = task.getStatus();

        // Validate end date >= start date
        if (task.getActualStartDate() != null && endDate.isBefore(task.getActualStartDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        task.setActualEndDate(endDate);
        task.setStatus(TaskStatus.DONE);

        task = taskRepository.save(task);
        laborManagementService.syncPayrollForTask(task);
        if (previousStatus != TaskStatus.DONE) {
            domainEventPublisher.publish(new TaskCompletedEvent(task, previousStatus));
        }
        log.info("Marked task {} as done", taskId);

        return mapToResponse(task);
    }

    /**
     * Cancel a task.
     */
    @Transactional
    public TaskResponse cancelTask(Integer taskId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureSeasonOpenForWorkspaceTask(task.getSeason(), false);

        task.setStatus(TaskStatus.CANCELLED);
        task = taskRepository.save(task);
        log.info("Cancelled task {}", taskId);

        return mapToResponse(task);
    }

    /**
     * Delete a task (hard delete, dev only).
     */
    @Transactional
    public void deleteTask(Integer taskId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        Task task = taskRepository.findByIdAndUser(taskId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureSeasonOpenForWorkspaceTask(task.getSeason(), false);

        taskRepository.delete(task);
        log.info("Deleted task {}", taskId);
    }

    /**
     * Get user's seasons for dropdown (minimal data).
     */
    @Transactional(readOnly = true)
    public List<SeasonMinimalResponse> getUserSeasons() {
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        return seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds).stream()
                .map(season -> SeasonMinimalResponse.builder()
                        .seasonId(season.getId())
                        .seasonName(season.getSeasonName())
                        .startDate(season.getStartDate())
                        .endDate(season.getEndDate())
                        .plannedHarvestDate(season.getPlannedHarvestDate())
                        .build())
                .toList();
    }

    // ==================== Helper Methods ====================

    private void ensureSeasonOpenForWorkspaceTask(Season season, boolean forCreate) {
        if (season == null || season.getStatus() == null) {
            return;
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            if (forCreate) {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_ADD_TASK);
            }
            throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_TASK);
        }
    }

    /**
     * Auto-update overdue tasks for a specific user.
     */
    private void updateOverdueTasksForUser(org.example.QuanLyMuaVu.module.identity.entity.User user) {
        List<Task> overdueCandidate = taskRepository.findByUserWithFilters(
                user,
                null,
                null,
                null,
                Pageable.unpaged()).getContent();

        for (Task task : overdueCandidate) {
            if (task.getDueDate() != null &&
                    task.getDueDate().isBefore(LocalDate.now()) &&
                    (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS)) {
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);
            }
        }
    }

    /**
     * Validate task dates.
     */
    private void validateTaskDates(LocalDate plannedDate, LocalDate dueDate,
            LocalDate actualStartDate, LocalDate actualEndDate) {
        // Due date must be >= planned date
        if (plannedDate != null && dueDate != null && dueDate.isBefore(plannedDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Actual end must be >= actual start
        if (actualStartDate != null && actualEndDate != null && actualEndDate.isBefore(actualStartDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * Map Task entity to TaskResponse DTO.
     */
    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .notes(task.getNotes())
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .createdAt(task.getCreatedAt())
                .build();
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User resolveWorkspaceAssignee(Long assigneeUserId, org.example.QuanLyMuaVu.module.identity.entity.User fallbackAssignee, Season season) {
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

        if (season != null) {
            boolean isSeasonEmployee = seasonEmployeeRepository.existsBySeason_IdAndEmployee_Id(season.getId(), assignee.getId());
            if (!isSeasonEmployee) {
                throw new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND);
            }
        }

        return assignee;
    }
}
