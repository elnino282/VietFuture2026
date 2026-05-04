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
import org.example.QuanLyMuaVu.module.season.dto.request.CreateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.FieldLogResponse;
import org.example.QuanLyMuaVu.module.season.service.FieldLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for recording and managing field logs linked to seasons
 * (business flow [3]) for farms where the current user is a member.
 * 
 * API Contract:
 * - GET /api/v1/field-logs?seasonId=&type=&q=&from=&to=&page=&size=
 * - GET /api/v1/field-logs/{id}
 * - POST /api/v1/field-logs (body: {seasonId, logDate, logType, notes})
 * - PUT /api/v1/field-logs/{id} (body: {logDate, logType, notes})
 * - DELETE /api/v1/field-logs/{id}
 */

@RestController
@RequestMapping("/api/v1/field-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class FieldLogController {

        FieldLogService fieldLogService;

        @Operation(summary = "List field logs", description = "List field logs for a given season of the current farmer")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @GetMapping
        public ApiResponse<PageResponse<FieldLogResponse>> listFieldLogs(
                        @Parameter(description = "Season ID (required)") @RequestParam(value = "seasonId") Integer seasonId,
                        @Parameter(description = "Filter by log type") @RequestParam(value = "type", required = false) String type,
                        @Parameter(description = "Search text in notes (min 2 chars)") @RequestParam(value = "q", required = false) String q,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
                        @Parameter(description = "Sort field") @RequestParam(value = "sort", defaultValue = "logDate,desc") String sort) {
                return ApiResponse.success(
                                fieldLogService.listFieldLogsForSeason(seasonId, from, to, type, q, page, size));
        }

        @Operation(summary = "Get field log detail", description = "Get field log detail if it belongs to a season of current farmer")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field log not found")
        })
        @GetMapping("/{id}")
        public ApiResponse<FieldLogResponse> getFieldLog(@PathVariable Integer id) {
                return ApiResponse.success(fieldLogService.getFieldLog(id));
        }

        @Operation(summary = "Create field log", description = "Create a new field log linked to a season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping
        public ApiResponse<FieldLogResponse> createFieldLog(@Valid @RequestBody CreateFieldLogRequest request) {
                return ApiResponse.success(fieldLogService.createFieldLog(request.getSeasonId(), request));
        }

        @Operation(summary = "Update field log", description = "Update field log content while season is still open")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field log not found")
        })
        @PutMapping("/{id}")
        public ApiResponse<FieldLogResponse> updateFieldLog(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateFieldLogRequest request) {
                return ApiResponse.success(fieldLogService.updateFieldLog(id, request));
        }

        @Operation(summary = "Delete field log", description = "Delete a field log if allowed by season constraints")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field log not found")
        })
        @DeleteMapping("/{id}")
        public ApiResponse<Void> deleteFieldLog(@PathVariable Integer id) {
                fieldLogService.deleteFieldLog(id);
                return ApiResponse.success(null);
        }
}
