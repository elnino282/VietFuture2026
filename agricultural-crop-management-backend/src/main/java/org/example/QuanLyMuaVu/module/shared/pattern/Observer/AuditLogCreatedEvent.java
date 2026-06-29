package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AuditLogCreatedEvent extends DomainEvent {

    private final Long id;
    private final String action;
    private final String module;
    private final String details;
    private final String ipAddress;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public AuditLogCreatedEvent(Long id, String action, String module, String details, String ipAddress, String createdBy, LocalDateTime createdAt) {
        super("AuditLog", id != null ? id.toString() : "unknown");
        this.id = id;
        this.action = action;
        this.module = module;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    @Override
    public String getEventType() {
        return "audit.event.created";
    }
}
