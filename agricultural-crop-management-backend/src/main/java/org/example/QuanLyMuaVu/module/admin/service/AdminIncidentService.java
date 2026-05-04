package org.example.QuanLyMuaVu.module.admin.service;

import static org.example.QuanLyMuaVu.Enums.IncidentStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.incident.dto.request.CancelIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.ResolveIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.request.TriageIncidentRequest;
import org.example.QuanLyMuaVu.module.incident.dto.response.IncidentResponse;
import org.example.QuanLyMuaVu.module.incident.mapper.IncidentMapper;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentQueryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin service for system-wide incident management.
 * Implements a state machine workflow for incident processing.
 * 
 * State Machine:
 * - OPEN -> IN_PROGRESS (via triage)
 * - OPEN -> CANCELLED (via cancel)
 * - IN_PROGRESS -> RESOLVED (via resolve)
 * - IN_PROGRESS -> CANCELLED (via cancel)
 * 
 * Note: This service is adapted to work with the current org.example.QuanLyMuaVu.module.incident.entity.Incident entity
 * structure.
 * Some advanced features (assignee, cancellation reason) are not stored in DB
 * but the workflow is fully functional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminIncidentService {

    IncidentQueryPort incidentQueryPort;
    IncidentCommandPort incidentCommandPort;
    IncidentMapper incidentMapper;

    // ═══════════════════════════════════════════════════════════════
    // STATE MACHINE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Allowed state transitions map.
     */
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            OPEN, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(RESOLVED, CANCELLED));

    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all incidents with optional filters.
     */
    public PageResponse<IncidentResponse> getAllIncidents(String status, String severity, String type, int page,
            int size) {
        log.info("Admin fetching all incidents - status: {}, severity: {}, type: {}, page: {}, size: {}",
                status, severity, type, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<org.example.QuanLyMuaVu.module.incident.entity.Incident> incidentPage = incidentQueryPort.findAllIncidents(pageable);

        List<IncidentResponse> content = incidentPage.getContent().stream()
                .filter(i -> status == null || (i.getStatus() != null && i.getStatus().name().equals(status)))
                .filter(i -> severity == null || (i.getSeverity() != null && i.getSeverity().name().equals(severity)))
                .filter(i -> type == null || (i.getIncidentType() != null && i.getIncidentType().equals(type)))
                .map(incidentMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(incidentPage, content);
    }

    /**
     * Get incident detail by ID.
     */
    public IncidentResponse getIncidentById(Integer incidentId) {
        log.info("Admin fetching incident detail for ID: {}", incidentId);

        org.example.QuanLyMuaVu.module.incident.entity.Incident incident = incidentQueryPort.findIncidentById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        return incidentMapper.toResponse(incident);
    }

    // ═══════════════════════════════════════════════════════════════
    // TRIAGE OPERATION (OPEN -> IN_PROGRESS)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Triage an open incident: set severity, deadline, and transition to
     * IN_PROGRESS.
     */
    @Transactional
    public IncidentResponse triage(Integer incidentId, TriageIncidentRequest request) {
        log.info("Admin triaging incident {} with severity: {}, deadline: {}",
                incidentId, request.getSeverity(), request.getDeadline());

        try {
            org.example.QuanLyMuaVu.module.incident.entity.Incident incident = incidentQueryPort.findIncidentById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be OPEN
            validateTransition(incident.getStatus(), IN_PROGRESS);

            // Validate deadline if provided
            if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.INVALID_DEADLINE);
            }

            // Update severity
            IncidentSeverity newSeverity = IncidentSeverity.fromCode(request.getSeverity());
            incident.setSeverity(newSeverity);

            // Set deadline if provided
            if (request.getDeadline() != null) {
                incident.setDeadline(request.getDeadline());
            }

            // Note: assignee is not stored in current entity structure
            // but the triage workflow is still functional

            // Transition to IN_PROGRESS
            incident.setStatus(IN_PROGRESS);

            incidentCommandPort.saveIncident(incident);
            log.info("org.example.QuanLyMuaVu.module.incident.entity.Incident {} triaged successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RESOLVE OPERATION (IN_PROGRESS -> RESOLVED)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolve an in-progress incident.
     */
    @Transactional
    public IncidentResponse resolve(Integer incidentId, ResolveIncidentRequest request) {
        log.info("Admin resolving incident {} with note: {}", incidentId, request.getResolutionNote());

        try {
            org.example.QuanLyMuaVu.module.incident.entity.Incident incident = incidentQueryPort.findIncidentById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be IN_PROGRESS
            validateTransition(incident.getStatus(), RESOLVED);

            // Set resolution details
            incident.setStatus(RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());

            // Note: resolutionNote and resolvedBy are not stored in current entity
            // structure
            // but logged for audit trail
            log.info("Resolution note for incident {}: {}", incidentId, request.getResolutionNote());

            incidentCommandPort.saveIncident(incident);
            log.info("org.example.QuanLyMuaVu.module.incident.entity.Incident {} resolved successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL OPERATION (OPEN/IN_PROGRESS -> CANCELLED)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cancel an incident.
     */
    @Transactional
    public IncidentResponse cancel(Integer incidentId, CancelIncidentRequest request) {
        log.info("Admin cancelling incident {} with reason: {}", incidentId, request.getCancellationReason());

        try {
            org.example.QuanLyMuaVu.module.incident.entity.Incident incident = incidentQueryPort.findIncidentById(incidentId)
                    .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

            // Validate transition: must be OPEN or IN_PROGRESS
            validateTransition(incident.getStatus(), CANCELLED);

            // Set cancellation status
            incident.setStatus(CANCELLED);

            // Note: cancellationReason is not stored in current entity structure
            // but logged for audit trail
            log.info("Cancellation reason for incident {}: {}", incidentId, request.getCancellationReason());

            incidentCommandPort.saveIncident(incident);
            log.info("org.example.QuanLyMuaVu.module.incident.entity.Incident {} cancelled successfully", incidentId);
            return incidentMapper.toResponse(incident);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for incident {}", incidentId);
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LEGACY STATUS UPDATE (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generic status update for legacy compatibility.
     */
    @Transactional
    public IncidentResponse updateStatus(Integer incidentId, String newStatus) {
        log.info("Admin updating incident {} status to: {}", incidentId, newStatus);

        org.example.QuanLyMuaVu.module.incident.entity.Incident incident = incidentQueryPort.findIncidentById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        IncidentStatus targetStatus = IncidentStatus.valueOf(newStatus);

        // Validate transition
        validateTransition(incident.getStatus(), targetStatus);

        incident.setStatus(targetStatus);

        // Set resolved_at when status changes to RESOLVED
        if (targetStatus == RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        incidentCommandPort.saveIncident(incident);
        return incidentMapper.toResponse(incident);
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validate that the transition from current status to target status is allowed.
     */
    private void validateTransition(IncidentStatus current, IncidentStatus target) {
        if (current == null) {
            throw new AppException(ErrorCode.INVALID_INCIDENT_STATUS_TRANSITION);
        }

        Set<IncidentStatus> allowedTargets = ALLOWED_TRANSITIONS.get(current);
        if (allowedTargets == null || !allowedTargets.contains(target)) {
            log.warn("Invalid transition attempted: {} -> {}", current, target);
            throw new AppException(ErrorCode.INVALID_INCIDENT_STATUS_TRANSITION);
        }
    }
}
