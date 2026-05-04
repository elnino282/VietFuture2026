package org.example.QuanLyMuaVu.module.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonStatusRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.service.SeasonService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for planning and managing seasons on plots (business flow
 * [2]),
 * including search, CRUD and status transitions for the current farmer.
 */

@RestController
@RequestMapping("/api/v1/seasons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class SeasonController {

        SeasonService seasonService;

        @Operation(summary = "Search seasons", description = "Search seasons of the current farmer by plot, crop, status and date range")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping
        public ApiResponse<PageResponse<SeasonResponse>> searchSeasons(
                        @RequestParam(value = "plotId", required = false) Integer plotId,
                        @RequestParam(value = "cropId", required = false) Integer cropId,
                        @RequestParam(value = "status", required = false) String status,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
                return ApiResponse.success(seasonService.searchMySeasons(plotId, cropId, status, from, to, page, size));
        }

        @Operation(summary = "Get my seasons (minimal)", description = "Get a minimal list of seasons for dropdown selectors")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping("/my")
        public ApiResponse<java.util.List<org.example.QuanLyMuaVu.module.season.dto.response.MySeasonResponse>> getMySeasons() {
                return ApiResponse.success(seasonService.getMySeasons());
        }

        @Operation(summary = "Get season detail", description = "Get season detail belonging to current farmer")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
        })
        @GetMapping("/{id}")
        public ApiResponse<SeasonDetailResponse> getSeason(@PathVariable Integer id) {
                return ApiResponse.success(seasonService.getSeasonForCurrentFarmer(id));
        }

        @Operation(summary = "Create season", description = "Create a new season for a given plot and crop")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
        })
        @PostMapping
        public ApiResponse<SeasonDetailResponse> createSeason(@Valid @RequestBody CreateSeasonRequest request) {
                // BR8: Call PascalCase wrapper method with season name uniqueness validation
                return ApiResponse.success(seasonService.CreateSeason(request));
        }

        @Operation(summary = "Update season", description = "Update season details while status is still editable")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
        })
        @PutMapping("/{id}")
        public ApiResponse<SeasonDetailResponse> updateSeason(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateSeasonRequest request) {
                // BR12: Call PascalCase wrapper method with season name uniqueness validation
                return ApiResponse.success(seasonService.UpdateSeason(id, request));
        }

        @Operation(summary = "Update season status", description = "Change season status (PLANNED → ACTIVE → COMPLETED/CANCELLED/ARCHIVED)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
        })
        @PatchMapping("/{id}/status")
        public ApiResponse<SeasonResponse> updateSeasonStatus(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateSeasonStatusRequest request) {
                return ApiResponse.success(seasonService.updateSeasonStatus(id, request));
        }

        @Operation(summary = "Start season", description = "Start a planned season (PLANNED → ACTIVE)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping("/{id}/start")
        public ApiResponse<SeasonResponse> startSeason(
                        @PathVariable Integer id,
                        @RequestBody(required = false) org.example.QuanLyMuaVu.module.season.dto.request.StartSeasonRequest request) {
                // BR23: Call PascalCase wrapper method
                return ApiResponse.success(seasonService.StartSeason(id, request));
        }

        @Operation(summary = "Complete season", description = "Complete an active season (ACTIVE → COMPLETED)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition or pending tasks exist"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping("/{id}/complete")
        public ApiResponse<SeasonResponse> completeSeason(
                        @PathVariable Integer id,
                        @Valid @RequestBody org.example.QuanLyMuaVu.module.season.dto.request.CompleteSeasonRequest request) {
                // BR27: Call PascalCase wrapper method
                return ApiResponse.success(seasonService.CompleteSeason(id, request));
        }

        @Operation(summary = "Cancel season", description = "Cancel a season (→ CANCELLED)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition or harvests exist"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping("/{id}/cancel")
        public ApiResponse<SeasonResponse> cancelSeason(
                        @PathVariable Integer id,
                        @RequestBody(required = false) org.example.QuanLyMuaVu.module.season.dto.request.CancelSeasonRequest request) {
                if (request == null) {
                        request = org.example.QuanLyMuaVu.module.season.dto.request.CancelSeasonRequest.builder().build();
                }
                // BR31: Call PascalCase wrapper method
                return ApiResponse.success(seasonService.CancelSeason(id, request));
        }

        @Operation(summary = "Delete season", description = "Delete a season if allowed by business constraints")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
        })
        @DeleteMapping("/{id}")
        public ApiResponse<Void> deleteSeason(@PathVariable Integer id) {
                seasonService.deleteSeason(id);
                return ApiResponse.success(null);
        }

        @Operation(summary = "Archive season", description = "BR15: Archive a completed or cancelled season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition - only COMPLETED or CANCELLED can be archived"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PatchMapping("/{id}/archive")
        public ApiResponse<SeasonResponse> archiveSeason(@PathVariable Integer id) {
                // BR14: Call ArchiveSeason() PascalCase method
                return ApiResponse.success(seasonService.ArchiveSeason(id));
        }

        @Operation(summary = "Search seasons by keyword", description = "BR17: Search seasons using Text_change() style keyword search")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping("/search")
        public ApiResponse<java.util.List<SeasonResponse>> searchSeasonsByKeyword(
                        @RequestParam(value = "q", required = false) String keyword) {
                return ApiResponse.success(seasonService.searchSeasonsByKeyword(keyword));
        }
}
