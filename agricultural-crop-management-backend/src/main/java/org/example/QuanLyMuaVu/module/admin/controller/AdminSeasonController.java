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
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminSeasonUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.service.AdminSeasonService;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for system-wide season monitoring and intervention.
 * Provides read-only access to all seasons and allows admin intervention
 * for completing seasons with automatic task cancellation.
 */
@RestController
@RequestMapping("/api/v1/admin/seasons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Seasons", description = "Admin endpoints for system-wide season monitoring and intervention")
public class AdminSeasonController {

    AdminSeasonService adminSeasonService;

    @Operation(summary = "List all seasons (Admin)", description = "Get paginated list of all seasons across the system with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<SeasonResponse>> listAllSeasons(
            @Parameter(description = "Filter by farm ID") @RequestParam(value = "farmId", required = false) Integer farmId,
            @Parameter(description = "Filter by status (PLANNED, ACTIVE, COMPLETED, CANCELLED, ARCHIVED)") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by crop ID") @RequestParam(value = "cropId", required = false) Integer cropId,
            @Parameter(description = "Filter by plot ID") @RequestParam(value = "plotId", required = false) Integer plotId,
            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(adminSeasonService.getAllSeasons(farmId, status, cropId, plotId, page, size));
    }

    @Operation(summary = "Get season detail (Admin)", description = "Get detailed information about a specific season including tasks, expenses, harvests, incidents")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<SeasonDetailResponse> getSeasonDetail(@PathVariable Integer id) {
        return ApiResponse.success(adminSeasonService.getSeasonById(id));
    }

    @Operation(summary = "Get pending task count (Admin)", description = "Get count of non-DONE tasks for a season. Used for UX warning when completing a season.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{id}/pending-task-count")
    public ApiResponse<Long> getPendingTaskCount(@PathVariable Integer id) {
        return ApiResponse.success(adminSeasonService.getPendingTaskCount(id));
    }

    @Operation(summary = "Update season (Admin Intervention)", description = "Update a season. If changing status to COMPLETED, actual_yield and end_date are required. All pending tasks will be auto-cancelled.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Missing yield/date for completion"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
    })
    @PutMapping("/{id}")
    public ApiResponse<SeasonResponse> updateSeason(
            @PathVariable Integer id,
            @Valid @RequestBody AdminSeasonUpdateRequest request) {
        return ApiResponse.success(adminSeasonService.updateSeason(id, request));
    }
}
