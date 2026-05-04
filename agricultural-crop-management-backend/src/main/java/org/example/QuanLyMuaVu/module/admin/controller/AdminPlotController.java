package org.example.QuanLyMuaVu.module.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminPlotQueryService;
import org.example.QuanLyMuaVu.module.farm.dto.response.PlotResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for system-wide plot management.
 * Returns all plots across all farms for administrative purposes.
 */
@RestController
@RequestMapping("/api/v1/admin/plots")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Plots", description = "Admin endpoints for system-wide plot management")
public class AdminPlotController {

    AdminPlotQueryService adminPlotQueryService;

    @Operation(summary = "List all plots (Admin)", description = "Get paginated list of all plots across all farms")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<PlotResponse>> listAllPlots(
            @Parameter(description = "Filter by farm ID") @RequestParam(value = "farmId", required = false) Integer farmId,
            @Parameter(description = "Search by name") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(adminPlotQueryService.listAllPlots(farmId, keyword, page, size));
    }

    @Operation(summary = "Get plot detail (Admin)", description = "Get detailed information about a specific plot")
    @GetMapping("/{id}")
    public ApiResponse<PlotResponse> getPlot(@PathVariable Integer id) {
        return ApiResponse.success(adminPlotQueryService.getPlot(id));
    }

    @Operation(summary = "Get plot seasons (Admin)", description = "Get all seasons for a specific plot")
    @GetMapping("/{id}/seasons")
    public ApiResponse<List<SeasonResponse>> listPlotSeasons(@PathVariable Integer id) {
        return ApiResponse.success(adminPlotQueryService.listPlotSeasons(id));
    }
}
