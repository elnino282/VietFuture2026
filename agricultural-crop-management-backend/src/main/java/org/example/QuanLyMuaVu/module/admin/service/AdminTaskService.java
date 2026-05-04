package org.example.QuanLyMuaVu.module.admin.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminTaskUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.port.TaskCommandPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminTaskService {

    TaskQueryPort taskQueryPort;
    TaskCommandPort taskCommandPort;
    IdentityQueryPort identityQueryPort;

    public PageResponse<TaskResponse> getAllTasks(
            Integer farmId,
            Integer cropId,
            Integer seasonId,
            String status,
            int page,
            int size) {
        log.info("Admin fetching all tasks - farmId: {}, cropId: {}, seasonId: {}, status: {}, page: {}, size: {}",
                farmId, cropId, seasonId, status, page, size);

        TaskStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = TaskStatus.fromCode(status);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid task status filter: {}", status);
            }
        }
        final TaskStatus effectiveStatusFilter = statusFilter;

        List<TaskResponse> filtered = taskQueryPort.findAllTasks().stream()
                .filter(task -> farmId == null
                        || (task.getSeason() != null
                                && task.getSeason().getPlot() != null
                                && task.getSeason().getPlot().getFarm() != null
                                && farmId.equals(task.getSeason().getPlot().getFarm().getId())))
                .filter(task -> cropId == null
                        || (task.getSeason() != null
                                && task.getSeason().getCrop() != null
                                && cropId.equals(task.getSeason().getCrop().getId())))
                .filter(task -> seasonId == null
                        || (task.getSeason() != null && seasonId.equals(task.getSeason().getId())))
                .filter(task -> effectiveStatusFilter == null || effectiveStatusFilter.equals(task.getStatus()))
                .map(this::toTaskResponse)
                .toList();

        return toPagedResponse(filtered, page, size);
    }

    public TaskResponse getTaskById(Integer taskId) {
        log.info("Admin fetching task detail for ID: {}", taskId);
        org.example.QuanLyMuaVu.module.season.entity.Task task = taskQueryPort.findTaskById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Integer taskId, AdminTaskUpdateRequest request) {
        log.info("Admin updating task {} with request: {}", taskId, request);

        org.example.QuanLyMuaVu.module.season.entity.Task task = taskQueryPort.findTaskById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.fromCode(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        if (request.getUserId() != null) {
            Long previousUserId = task.getUser() != null ? task.getUser().getId() : null;
            if (!request.getUserId().equals(previousUserId)) {
                org.example.QuanLyMuaVu.module.farm.entity.Farm farm = getFarmFromTask(task);
                if (farm == null) {
                    throw new AppException(ErrorCode.BAD_REQUEST);
                }
                if (farm.getUser() == null || !farm.getUser().getId().equals(request.getUserId())) {
                    log.warn("org.example.QuanLyMuaVu.module.season.entity.Task {} assigned to non-owner user {}", taskId, request.getUserId());
                }
                org.example.QuanLyMuaVu.module.identity.entity.User newUser = identityQueryPort.findUserById(request.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                task.setUser(newUser);
                log.info("Admin Intervention: org.example.QuanLyMuaVu.module.season.entity.Task {} reassigned from user {} to user {}",
                        taskId, previousUserId, request.getUserId());
            }
        }

        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        org.example.QuanLyMuaVu.module.season.entity.Task saved = taskCommandPort.saveTask(task);
        return toTaskResponse(saved);
    }

    private org.example.QuanLyMuaVu.module.farm.entity.Farm getFarmFromTask(org.example.QuanLyMuaVu.module.season.entity.Task task) {
        if (task.getSeason() != null && task.getSeason().getPlot() != null) {
            return task.getSeason().getPlot().getFarm();
        }
        return null;
    }

    private TaskResponse toTaskResponse(org.example.QuanLyMuaVu.module.season.entity.Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .notes(task.getNotes())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private PageResponse<TaskResponse> toPagedResponse(List<TaskResponse> allItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = safePage * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, allItems.size());
        List<TaskResponse> pageItems = fromIndex >= allItems.size() ? List.of() : allItems.subList(fromIndex, toIndex);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return PageResponse.of(new PageImpl<>(pageItems, pageable, allItems.size()), pageItems);
    }
}
