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
