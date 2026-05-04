package org.example.QuanLyMuaVu.module.admin.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.admin.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        baseTime = LocalDateTime.of(2026, 4, 26, 10, 0, 0);

        auditLogRepository.saveAll(List.of(
                AuditLog.builder()
                        .entityType("IDENTITY_USER")
                        .entityId(12)
                        .operation("RBAC_STATUS_UPDATED")
                        .performedBy("admin-main")
                        .performedAt(baseTime.minusHours(1))
                        .snapshotDataJson("{\"status\":\"LOCKED\"}")
                        .reason("Status locked")
                        .ipAddress("127.0.0.1")
                        .build(),
                AuditLog.builder()
                        .entityType("IDENTITY_USER")
                        .entityId(15)
                        .operation("RBAC_ROLE_UPDATED")
                        .performedBy("admin-second")
                        .performedAt(baseTime.minusHours(2))
                        .snapshotDataJson("{\"roles\":[\"ADMIN\"]}")
                        .reason("Role updated")
                        .ipAddress("127.0.0.1")
                        .build(),
                AuditLog.builder()
                        .entityType("INVENTORY_STOCK_MOVEMENT")
                        .entityId(101)
                        .operation("INVENTORY_ADJUSTED")
                        .performedBy("farmer-owner")
                        .performedAt(baseTime.minusHours(3))
                        .snapshotDataJson("{\"quantity\":10}")
                        .reason("Inventory reconciliation")
                        .ipAddress("127.0.0.1")
                        .build()));
    }

    @Test
    void searchAuditLogs_filtersByModuleActionUserAndTime() {
        Page<AuditLog> result = auditLogRepository.searchAuditLogs(
                baseTime.minusHours(2),
                baseTime,
                "IDENTITY",
                null,
                "RBAC_STATUS_UPDATED",
                "admin",
                null,
                PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("IDENTITY_USER", result.getContent().get(0).getEntityType());
        assertEquals("RBAC_STATUS_UPDATED", result.getContent().get(0).getOperation());
    }

    @Test
    void searchAuditLogs_filtersByEntityTypeAndEntityId() {
        Page<AuditLog> result = auditLogRepository.searchAuditLogs(
                null,
                null,
                null,
                "INVENTORY_STOCK_MOVEMENT",
                null,
                null,
                101,
                PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(log -> "INVENTORY_STOCK_MOVEMENT".equals(log.getEntityType())));
    }
}
