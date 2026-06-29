package org.example.adminreporting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.response.AdminAuditLogResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.entity.AuditLogEntry;
import org.example.adminreporting.repository.AuditLogEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogEntryRepository auditLogEntryRepository;

    public PageResponse<AdminAuditLogResponse> listAuditLogs(
            LocalDateTime fromTime,
            LocalDateTime toTime,
            String module,
            String entityType,
            String operation,
            String performedBy,
            Integer entityId,
            Pageable pageable) {

        Pageable safePageable = withDefaultSort(pageable);

        Page<AuditLogEntry> logsPage = auditLogEntryRepository.searchAuditLogs(
                fromTime,
                toTime,
                normalizeFilter(module),
                normalizeFilter(entityType),
                normalizeFilter(operation),
                normalizeFilter(performedBy),
                entityId,
                safePageable);

        List<AdminAuditLogResponse> items = logsPage.getContent().stream()
                .map(this::toAuditLogResponse)
                .toList();

        return PageResponse.of(logsPage, items);
    }

    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "performedAt"));
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "performedAt"));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminAuditLogResponse toAuditLogResponse(AuditLogEntry auditLog) {
        return AdminAuditLogResponse.builder()
                .id(auditLog.getId())
                .module(extractModule(auditLog.getEntityType()))
                .operation(auditLog.getOperation())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .performedBy(auditLog.getPerformedBy())
                .performedAt(auditLog.getPerformedAt())
                .snapshotData(auditLog.getSnapshotDataJson())
                .reason(auditLog.getReason())
                .ipAddress(auditLog.getIpAddress())
                .build();
    }

    private String extractModule(String entityType) {
        String normalizedEntityType = normalizeFilter(entityType);
        if (normalizedEntityType == null) {
            return "GENERAL";
        }
        int separatorIndex = normalizedEntityType.indexOf('_');
        if (separatorIndex < 0) {
            return normalizedEntityType.toUpperCase(Locale.ROOT);
        }
        return normalizedEntityType.substring(0, separatorIndex).toUpperCase(Locale.ROOT);
    }
}
