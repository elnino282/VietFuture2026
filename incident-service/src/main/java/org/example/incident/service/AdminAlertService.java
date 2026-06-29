package org.example.incident.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.incident.dto.common.PageResponse;
import org.example.incident.exception.AppException;
import org.example.incident.exception.ErrorCode;
import org.example.incident.dto.request.AdminAlertSendRequest;
import org.example.incident.dto.response.AdminAlertResponse;
import org.example.incident.entity.Alert;
import org.example.incident.port.IncidentCommandPort;
import org.example.incident.port.IncidentQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class AdminAlertService {

    IncidentQueryPort incidentQueryPort;
    IncidentCommandPort incidentCommandPort;
    ExternalServiceClient externalServiceClient;

    @Transactional(readOnly = true)
    public PageResponse<AdminAlertResponse> listAlerts(
            String type,
            String severity,
            String status,
            Integer farmId,
            Integer windowDays,
            Integer page,
            Integer limit) {

        int safePage = Math.max(page != null ? page : 0, 0);
        int safeLimit = normalizeLimit(limit);
        LocalDateTime fromDate = resolveFromDate(windowDays);

        Pageable pageable = PageRequest.of(
                safePage,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Alert> alertsPage = incidentQueryPort.searchAlerts(
                normalizeNullable(type),
                normalizeNullable(severity),
                normalizeNullable(status),
                farmId,
                fromDate,
                pageable);

        List<AdminAlertResponse> items = alertsPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(alertsPage, items);
    }

    @Transactional(readOnly = true)
    public List<AdminAlertResponse> refreshAlerts(Integer windowDays) {
        LocalDateTime fromDate = resolveFromDate(windowDays);

        Page<Alert> alertsPage = incidentQueryPort.searchAlerts(
                null,
                null,
                null,
                null,
                fromDate,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")));

        return alertsPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AdminAlertResponse sendAlert(Integer alertId, AdminAlertSendRequest request) {
        Alert alert = incidentQueryPort.findAlertById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        alert.setStatus("SENT");
        alert.setSentAt(LocalDateTime.now());

        if (request != null && request.getRecipientFarmerIds() != null && !request.getRecipientFarmerIds().isEmpty()) {
            String csv = request.getRecipientFarmerIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            alert.setRecipientFarmerIds(csv);
        }

        Alert saved = incidentCommandPort.saveAlert(alert);
        return toResponse(saved);
    }

    public AdminAlertResponse updateStatus(Integer alertId, String status) {
        Alert alert = incidentQueryPort.findAlertById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        String normalizedStatus = normalizeRequiredStatus(status);
        alert.setStatus(normalizedStatus);
        if ("SENT".equals(normalizedStatus) && alert.getSentAt() == null) {
            alert.setSentAt(LocalDateTime.now());
        }

        Alert saved = incidentCommandPort.saveAlert(alert);
        return toResponse(saved);
    }

    private AdminAlertResponse toResponse(Alert alert) {
        String farmName = null;
        if (alert.getFarmId() != null) {
            ExternalServiceClient.FarmInternalDto farm = externalServiceClient.getFarm(alert.getFarmId());
            if (farm != null) {
                farmName = farm.getName();
            }
        }
        return AdminAlertResponse.builder()
                .id(alert.getId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .farmId(alert.getFarmId())
                .farmName(farmName)
                .seasonId(alert.getSeasonId())
                .plotId(alert.getPlotId())
                .cropId(alert.getCropId())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .suggestedActionType(alert.getSuggestedActionType())
                .suggestedActionUrl(alert.getSuggestedActionUrl())
                .recipientFarmerIds(parseRecipientIds(alert.getRecipientFarmerIds()))
                .createdAt(alert.getCreatedAt())
                .sentAt(alert.getSentAt())
                .build();
    }

    private List<Integer> parseRecipientIds(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                })
                .filter(value -> value != null)
                .toList();
    }

    private LocalDateTime resolveFromDate(Integer windowDays) {
        if (windowDays == null || windowDays <= 0) {
            return null;
        }
        return LocalDateTime.now().minusDays(windowDays);
    }

    private int normalizeLimit(Integer limit) {
        int resolved = limit != null ? limit : 20;
        if (resolved <= 0) {
            return 20;
        }
        return Math.min(resolved, 100);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
