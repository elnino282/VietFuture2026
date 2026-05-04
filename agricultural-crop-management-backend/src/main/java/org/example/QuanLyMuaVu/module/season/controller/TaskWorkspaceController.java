package org.example.QuanLyMuaVu.module.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.StartTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.TaskDoneRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateTaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonMinimalResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.service.TaskWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for Tasks Workspace (user-scoped task management).
 */
@RestController
@RequestMapping("/api/v1/workspace/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class TaskWorkspaceController {

    TaskWorkspaceService taskWorkspaceService;

    @Operation(summary = "Create task", description = "Create a new task with optional season link")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskWorkspaceService.createTask(request));
    }

    @Operation(summary = "List tasks", description = "List all tasks for current user with filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ApiResponse<PageResponse<TaskResponse>> listTasks(
            @Parameter(description = "Filter by status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by season ID") @RequestParam(required = false) Integer seasonId,
            @Parameter(description = "Search by title (min 2 chars)") @RequestParam(required = false) String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return ApiResponse.success(taskWorkspaceService.listTasks(
                status, seasonId, q, page, size, sortBy, sortDirection
        ));
    }

    @Operation(summary = "Get task", description = "Get task details by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Integer id) {
        return ApiResponse.success(taskWorkspaceService.getTask(id));
    }

    @Operation(summary = "Update task", description = "Update task details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ApiResponse.success(taskWorkspaceService.updateTask(id, request));
    }

    @Operation(summary = "Start task", description = "Mark task as started (IN_PROGRESS)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PatchMapping("/{id}/start")
    public ApiResponse<TaskResponse> startTask(
            @PathVariable Integer id,
            @RequestBody(required = false) StartTaskRequest request
    ) {
        return ApiResponse.success(taskWorkspaceService.startTask(id, request != null ? request : new StartTaskRequest()));
    }

    @Operation(summary = "Mark task as done", description = "Mark task as completed (DONE)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PatchMapping("/{id}/done")
    public ApiResponse<TaskResponse> doneTask(
            @PathVariable Integer id,
            @RequestBody(required = false) TaskDoneRequest request
    ) {
        return ApiResponse.success(taskWorkspaceService.doneTask(id, request != null ? request : new TaskDoneRequest()));
    }

    @Operation(summary = "Cancel task", description = "Mark task as cancelled (CANCELLED)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PatchMapping("/{id}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@PathVariable Integer id) {
        return ApiResponse.success(taskWorkspaceService.cancelTask(id));
    }

    @Operation(summary = "Delete task", description = "Permanently delete task (dev only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Integer id) {
        taskWorkspaceService.deleteTask(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Get user seasons", description = "Get minimal season list for dropdown")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/seasons")
    public ApiResponse<List<SeasonMinimalResponse>> getUserSeasons() {
        return ApiResponse.success(taskWorkspaceService.getUserSeasons());
    }
}
