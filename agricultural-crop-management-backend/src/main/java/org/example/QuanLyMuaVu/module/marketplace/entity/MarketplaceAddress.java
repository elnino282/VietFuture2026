package org.example.QuanLyMuaVu.module.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "marketplace_addresses")
public class MarketplaceAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User user;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(name = "phone", nullable = false, length = 30)
    String phone;

    @Column(name = "province", nullable = false, length = 120)
    String province;

    @Column(name = "district", nullable = false, length = 120)
    String district;

    @Column(name = "ward", nullable = false, length = 120)
    String ward;

    @Column(name = "street", nullable = false, length = 255)
    String street;

    @Column(name = "detail", length = 500)
    String detail;

    @Column(name = "label", nullable = false, length = 30)
    String label;

    @Column(name = "is_default", nullable = false)
    Boolean isDefault;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (label == null || label.isBlank()) {
            label = "home";
        }
        if (isDefault == null) {
            isDefault = Boolean.FALSE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
