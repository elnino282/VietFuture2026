package org.example.QuanLyMuaVu.module.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminAuditLogResponse;
import org.example.QuanLyMuaVu.module.admin.entity.AuditLog;
import org.example.QuanLyMuaVu.module.admin.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared infrastructure for audit logging and admin audit-viewer queries.
 * 
 * Notes:
 * - Write APIs are internal only (no edit/delete API exposed).
 * - Snapshot JSON is redacted for sensitive fields before persistence.
 * - Existing entry point {@link #logEntityOperation(String, Integer, String, String, Object, String, String)}
 * remains for backward compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {

    Set<String> SENSITIVE_FIELD_KEYWORDS = Set.of(
            "password",
            "pwd",
            "token",
            "secret",
            "authorization",
            "apikey",
            "api_key",
            "accesskey",
            "refresh_token",
            "credential");
    String REDACTED_VALUE = "[REDACTED]";

    AuditLogRepository auditLogRepository;
    ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFarmOperation(
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm,
            String operation,
            String performedBy,
            String reason,
            String ipAddress) {
        if (farm == null || farm.getId() == null) {
            return;
        }
        try {
            persistAuditLog(
                    "FARM",
                    "FARM",
                    farm.getId(),
                    operation,
                    performedBy,
                    farm,
                    reason,
                    ipAddress);
        } catch (Exception e) {
            log.error(
                    "[AUDIT_FAILURE] Failed to create audit log for farm operation: farmId={}, operation={}, error={}",
                    farm.getId(),
                    operation,
                    e.getMessage(),
                    e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logFarmOperationCritical(
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm,
            String operation,
            String performedBy,
            String reason,
            String ipAddress) {
        if (farm == null || farm.getId() == null) {
            return;
        }
        try {
            persistAuditLog(
                    "FARM",
                    "FARM",
                    farm.getId(),
                    operation,
                    performedBy,
                    farm,
                    reason,
                    ipAddress);
        } catch (Exception e) {
            log.error("[AUDIT_CRITICAL_FAILURE] Failed to create critical audit log: farmId={}, operation={}, error={}",
                    farm.getId(),
                    operation,
                    e.getMessage(),
                    e);
            throw new RuntimeException("Critical audit logging failed for " + operation + " on FARM " + farm.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEntityOperation(
            String entityType,
            Integer entityId,
            String operation,
            String performedBy,
            Object snapshot,
            String reason,
            String ipAddress) {
        try {
            persistAuditLog(
                    null,
                    entityType,
                    entityId,
                    operation,
                    performedBy,
                    snapshot,
                    reason,
                    ipAddress);
        } catch (Exception e) {
            log.error(
                    "[AUDIT_FAILURE] Failed to create audit log: entityType={}, entityId={}, operation={}, error={}",
                    entityType,
                    entityId,
                    operation,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Generic helper for modules to log auditable operations with explicit module ownership.
     * Entity type will be normalized to {@code MODULE_ENTITY} if it isn't already namespaced.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logModuleOperation(
            String module,
            String entityType,
            Integer entityId,
            String operation,
            String performedBy,
            Object snapshot,
            String reason,
            String ipAddress) {
        try {
            persistAuditLog(
                    module,
                    entityType,
                    entityId,
                    operation,
                    performedBy,
                    snapshot,
                    reason,
                    ipAddress);
        } catch (Exception e) {
            log.error(
                    "[AUDIT_FAILURE] Failed to create module audit log: module={}, entityType={}, entityId={}, operation={}, error={}",
                    module,
                    entityType,
                    entityId,
                    operation,
                    e.getMessage(),
                    e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getFarmAuditTrail(Integer farmId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc("FARM", farmId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getEntityAuditTrail(String entityType, Integer entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
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

        Page<AuditLog> logsPage = auditLogRepository.searchAuditLogs(
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

    private void persistAuditLog(
            String module,
            String entityType,
            Integer entityId,
            String operation,
            String performedBy,
            Object snapshot,
            String reason,
            String ipAddress) throws JsonProcessingException {
        if (entityId == null || operation == null || operation.isBlank()) {
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(normalizeEntityType(module, entityType))
                .entityId(entityId)
                .operation(operation.trim().toUpperCase(Locale.ROOT))
                .performedBy(resolveActor(performedBy))
                .performedAt(LocalDateTime.now())
                .snapshotDataJson(serializeAndRedactSnapshot(snapshot))
                .reason(normalizeFilter(reason))
                .ipAddress(resolveIpAddress(ipAddress))
                .build();

        auditLogRepository.save(auditLog);
    }

    private String serializeAndRedactSnapshot(Object snapshot) throws JsonProcessingException {
        if (snapshot == null) {
            return null;
        }
        JsonNode rawNode = objectMapper.valueToTree(snapshot);
        JsonNode redacted = redactSensitiveNode(rawNode);
        return objectMapper.writeValueAsString(redacted);
    }

    private JsonNode redactSensitiveNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }

        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveField(fieldName)) {
                    objectNode.put(fieldName, REDACTED_VALUE);
                    return;
                }
                objectNode.set(fieldName, redactSensitiveNode(value));
            });
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                arrayNode.add(redactSensitiveNode(item));
            }
            return arrayNode;
        }

        return node;
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private String normalizeEntityType(String module, String entityType) {
        String normalizedEntityType = normalizeFilter(entityType);
        if (normalizedEntityType == null) {
            normalizedEntityType = "UNKNOWN";
        }
        normalizedEntityType = normalizedEntityType.toUpperCase(Locale.ROOT);

        String normalizedModule = normalizeFilter(module);
        if (normalizedModule == null) {
            return normalizedEntityType;
        }
        normalizedModule = normalizedModule.toUpperCase(Locale.ROOT);

        if (normalizedEntityType.equals(normalizedModule)
                || normalizedEntityType.startsWith(normalizedModule + "_")) {
            return normalizedEntityType;
        }
        return normalizedModule + "_" + normalizedEntityType;
    }

    private String resolveActor(String performedBy) {
        String explicitActor = normalizeFilter(performedBy);
        if (explicitActor != null) {
            return explicitActor;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception ex) {
            log.debug("Could not resolve actor from SecurityContext: {}", ex.getMessage());
        }

        return "system";
    }

    private String resolveIpAddress(String ipAddress) {
        String explicitIp = normalizeFilter(ipAddress);
        if (explicitIp != null) {
            return explicitIp;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwardedFor = normalizeFilter(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = normalizeFilter(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return normalizeFilter(request.getRemoteAddr());
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

    private AdminAuditLogResponse toAuditLogResponse(AuditLog auditLog) {
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
