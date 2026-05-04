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
import org.example.QuanLyMuaVu.module.admin.service.AdminIncidentService;
import org.example.QuanLyMuaVu.module.incident.dto.request.CancelIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.ResolveIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.TriageIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.UpdateIncidentStatusRequest;
import org.example.QuanLyMuaVu.module.incident.dto.response.IncidentResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for system-wide incident management.
 * Implements state machine workflow: OPEN -> IN_PROGRESS -> RESOLVED (with
 * optional CANCELLED)
 * 
 * State Machine:
 * - OPEN -> IN_PROGRESS (via triage)
 * - OPEN -> CANCELLED (via cancel)
 * - IN_PROGRESS -> RESOLVED (via resolve)
 * - IN_PROGRESS -> CANCELLED (via cancel)
 */
@RestController
@RequestMapping("/api/v1/admin/incidents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Incidents", description = "Admin endpoints for system-wide incident management with state machine workflow")
public class AdminIncidentController {

    AdminIncidentService adminIncidentService;

    // ═══════════════════════════════════════════════════════════════
    // QUERY ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    @Operation(summary = "List all incidents (Admin)", description = "Get paginated list of all incidents across the system with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<IncidentResponse>> listAllIncidents(
            @Parameter(description = "Filter by status (OPEN, IN_PROGRESS, RESOLVED, CANCELLED)") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by severity (LOW, MEDIUM, HIGH)") @RequestParam(value = "severity", required = false) String severity,
            @Parameter(description = "Filter by incident type") @RequestParam(value = "type", required = false) String type,
            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(adminIncidentService.getAllIncidents(status, severity, type, page, size));
    }

    @Operation(summary = "Get incident detail (Admin)", description = "Get detailed information about a specific incident")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Incident not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<IncidentResponse> getIncidentDetail(@PathVariable Integer id) {
        return ApiResponse.success(adminIncidentService.getIncidentById(id));
    }

    // ═══════════════════════════════════════════════════════════════
    // WORKFLOW ENDPOINTS (State Machine)
    // ═══════════════════════════════════════════════════════════════

    @Operation(summary = "Triage incident (OPEN -> IN_PROGRESS)", description = "Triage an open incident: set severity, deadline, and optionally assign to a user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition or deadline"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Incident not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    })
    @PatchMapping("/{id}/triage")
    public ApiResponse<IncidentResponse> triageIncident(
            @PathVariable Integer id,
            @Valid @RequestBody TriageIncidentRequest request) {
        return ApiResponse.success(adminIncidentService.triage(id, request));
    }

    @Operation(summary = "Resolve incident (IN_PROGRESS -> RESOLVED)", description = "Resolve an in-progress incident with a resolution note")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Incident not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    })
    @PatchMapping("/{id}/resolve")
    public ApiResponse<IncidentResponse> resolveIncident(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveIncidentRequest request) {
        return ApiResponse.success(adminIncidentService.resolve(id, request));
    }

    @Operation(summary = "Cancel incident (OPEN/IN_PROGRESS -> CANCELLED)", description = "Cancel an incident with a reason (e.g., duplicate, invalid)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Incident not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    })
    @PatchMapping("/{id}/cancel")
    public ApiResponse<IncidentResponse> cancelIncident(
            @PathVariable Integer id,
            @Valid @RequestBody CancelIncidentRequest request) {
        return ApiResponse.success(adminIncidentService.cancel(id, request));
    }

    // ═══════════════════════════════════════════════════════════════
    // LEGACY ENDPOINT (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    @Operation(summary = "Update incident status (Legacy)", description = "Generic status update - prefer using specific endpoints: /triage, /resolve, /cancel")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Incident not found")
    })
    @PatchMapping("/{id}/status")
    public ApiResponse<IncidentResponse> updateIncidentStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateIncidentStatusRequest request) {
        return ApiResponse.success(adminIncidentService.updateStatus(id, request.getStatus()));
    }
}
