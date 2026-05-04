package org.example.QuanLyMuaVu.module.incident.controller;

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
import org.example.QuanLyMuaVu.module.incident.dto.request.CreateIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.IncidentStatusUpdateRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.UpdateIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.response.IncidentResponse;
import org.example.QuanLyMuaVu.module.incident.service.IncidentService;
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
 * REST endpoints for managing farm incidents.
 * Base path: /api/v1/incidents
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class IncidentController {

    IncidentService incidentService;

    @Operation(summary = "List incidents", description = "List incidents with pagination and filters. Requires seasonId.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - seasonId required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<IncidentResponse>> listIncidents(
            @Parameter(description = "Season ID (required)", required = true) @RequestParam("seasonId") Integer seasonId,

            @Parameter(description = "Filter by status: OPEN, IN_PROGRESS, RESOLVED, CANCELLED") @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "Filter by severity: LOW, MEDIUM, HIGH") @RequestParam(value = "severity", required = false) String severity,

            @Parameter(description = "Filter by incident type") @RequestParam(value = "type", required = false) String type,

            @Parameter(description = "Search query (min 2 chars, searches description)") @RequestParam(value = "q", required = false) String q,

            @Parameter(description = "Date range start (yyyy-MM-dd), filters createdAt") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Date range end (yyyy-MM-dd), filters createdAt") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,

            @Parameter(description = "Sort field, prefix with - for desc (e.g. -createdAt)") @RequestParam(value = "sort", required = false) String sort) {
        return ApiResponse.success(
                incidentService.listIncidents(seasonId, status, severity, type, q, from, to, page, size, sort));
    }

    @Operation(summary = "Get incident by ID", description = "Get a single incident by its ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<IncidentResponse> getIncident(@PathVariable Integer id) {
        return ApiResponse.success(incidentService.getById(id));
    }

    @Operation(summary = "Create incident", description = "Create a new incident. seasonId is required in body.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
    })
    @PostMapping
    public ApiResponse<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        return ApiResponse.success(incidentService.create(request.getSeasonId(), request));
    }

    @Operation(summary = "Update incident", description = "Update incident details (type, severity, description, deadline)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @PutMapping("/{id}")
    public ApiResponse<IncidentResponse> updateIncident(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateIncidentRequest request) {
        return ApiResponse.success(incidentService.update(id, request));
    }

    @Operation(summary = "Update incident status", description = "Change incident status. Requires resolutionNote for RESOLVED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - invalid status transition or missing resolutionNote"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @PatchMapping("/{id}/status")
    public ApiResponse<IncidentResponse> updateIncidentStatus(
            @PathVariable Integer id,
            @Valid @RequestBody IncidentStatusUpdateRequest request) {
        return ApiResponse.success(incidentService.updateStatus(id, request));
    }

    @Operation(summary = "Delete incident", description = "Delete an incident. Cannot delete resolved incidents.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - cannot delete resolved incident"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteIncident(@PathVariable Integer id) {
        incidentService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Get incident summary", description = "Get counts of incidents by status for a season")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/summary")
    public ApiResponse<IncidentService.IncidentSummary> getIncidentSummary(
            @RequestParam("seasonId") Integer seasonId) {
        return ApiResponse.success(incidentService.getSummary(seasonId));
    }
}
