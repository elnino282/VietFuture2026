package org.example.QuanLyMuaVu.module.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "marketplace_order_groups")
public class MarketplaceOrderGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "group_code", nullable = false, length = 64, unique = true)
    String groupCode;

    @ManyToOne
    @JoinColumn(name = "buyer_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User buyerUser;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 128)
    String requestFingerprint;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
