package org.example.QuanLyMuaVu.module.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminTaskUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.service.AdminTaskService;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for system-wide task monitoring and intervention.
 * Provides read-access to all tasks across all seasons and admin intervention
 * capabilities.
 */
@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Tasks", description = "Admin endpoints for system-wide task monitoring and intervention")
public class AdminTaskController {

    AdminTaskService adminTaskService;

    @Operation(summary = "List all tasks (Admin)", description = "Get paginated list of all tasks across all seasons with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<TaskResponse>> listAllTasks(
            @Parameter(description = "Filter by farm ID") @RequestParam(value = "farmId", required = false) Integer farmId,
            @Parameter(description = "Filter by crop ID") @RequestParam(value = "cropId", required = false) Integer cropId,
            @Parameter(description = "Filter by season ID") @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(adminTaskService.getAllTasks(farmId, cropId, seasonId, status, page, size));
    }

    @Operation(summary = "Get task detail (Admin)", description = "Get detailed information about a specific task")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Integer id) {
        return ApiResponse.success(adminTaskService.getTaskById(id));
    }

    @Operation(summary = "Update task (Admin Intervention)", description = "Update a task including status change and user reassignment. User must be the farm owner.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid assignee"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Integer id,
            @Valid @RequestBody AdminTaskUpdateRequest request) {
        return ApiResponse.success(adminTaskService.updateTask(id, request));
    }
}
