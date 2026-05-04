package org.example.QuanLyMuaVu.module.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Audit log entity for tracking critical operations across the system.
 * Records WHO did WHAT, WHEN, and retains a snapshot of data before the
 * operation.
 * Essential for compliance, debugging, and manual recovery in case of
 * accidental deletions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_entity_lookup", columnList = "entity_type,entity_id"),
        @Index(name = "idx_performed_at", columnList = "performed_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    String entityType; // "FARM", "PLOT", "SEASON", etc.

    @Column(name = "entity_id", nullable = false)
    Integer entityId;

    @Column(name = "operation", nullable = false, length = 50)
    String operation; // "SOFT_DELETE", "HARD_DELETE", "RESTORE", "CREATE", "UPDATE"

    @Column(name = "performed_by", nullable = false, length = 255)
    String performedBy; // username

    @Column(name = "performed_at", nullable = false)
    LocalDateTime performedAt;

    @Column(name = "snapshot_data", columnDefinition = "TEXT")
    String snapshotDataJson; // JSON snapshot of entity before operation

    @Column(name = "reason", length = 500)
    String reason; // User-provided reason for the operation

    @Column(name = "ip_address", length = 45)
    String ipAddress; // IPv4 (15 chars) or IPv6 (45 chars)
}
