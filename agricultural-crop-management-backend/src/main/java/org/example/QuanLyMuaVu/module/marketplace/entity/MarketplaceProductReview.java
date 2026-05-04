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
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "marketplace_product_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uq_review_order_item_buyer", columnNames = {"order_item_id", "buyer_user_id"})
})
public class MarketplaceProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    MarketplaceProduct product;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    MarketplaceOrder order;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    MarketplaceOrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "buyer_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User buyerUser;

    @Column(name = "rating", nullable = false)
    Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

    @Column(name = "hidden", nullable = false)
    @Builder.Default
    Boolean hidden = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
