package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.time.LocalDateTime;

public record MarketplaceOrderAuditLogResponse(
        Long id,
        String entityType,
        Integer entityId,
        String operation,
        String performedBy,
        LocalDateTime performedAt,
        String snapshotDataJson,
        String reason,
        String ipAddress) {
}
