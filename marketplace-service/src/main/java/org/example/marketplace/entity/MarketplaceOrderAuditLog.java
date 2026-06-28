package org.example.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "marketplace_order_audit_logs")
public class MarketplaceOrderAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "entity_type", nullable = false, length = 64)
    String entityType;

    @Column(name = "entity_id", nullable = false)
    Long entityId;

    @Column(name = "operation", nullable = false, length = 64)
    String operation;

    @Column(name = "performed_by")
    Long performedBy;

    @Column(name = "performed_at", nullable = false)
    LocalDateTime performedAt;

    @Column(name = "snapshot_data_json", columnDefinition = "TEXT")
    String snapshotDataJson;

    @Column(name = "reason", length = 255)
    String reason;

    @Column(name = "ip_address", length = 45)
    String ipAddress;
}
