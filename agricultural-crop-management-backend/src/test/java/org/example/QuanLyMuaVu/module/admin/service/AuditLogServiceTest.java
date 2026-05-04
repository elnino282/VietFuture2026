package org.example.QuanLyMuaVu.module.admin.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.QuanLyMuaVu.module.admin.entity.AuditLog;
import org.example.QuanLyMuaVu.module.admin.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void logEntityOperation_redactsSensitiveFieldsInSnapshot() {
        AuditLogService auditLogService = new AuditLogService(auditLogRepository, new ObjectMapper());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("username", "target-user");
        snapshot.put("password", "super-secret-password");
        snapshot.put("accessToken", "raw-token-value");
        snapshot.put("note", "safe-note");

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.logEntityOperation(
                "IDENTITY_USER",
                12,
                "RBAC_STATUS_UPDATED",
                "admin-user",
                snapshot,
                "status changed",
                "127.0.0.1");

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        String persistedSnapshot = auditLogCaptor.getValue().getSnapshotDataJson();
        assertTrue(persistedSnapshot.contains("[REDACTED]"));
        assertFalse(persistedSnapshot.contains("super-secret-password"));
        assertFalse(persistedSnapshot.contains("raw-token-value"));
    }
}
