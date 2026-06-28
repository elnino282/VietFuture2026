package org.example.marketplace.entity;

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
import java.math.BigDecimal;
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
@Table(name = "marketplace_cart_items")
public class MarketplaceCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    MarketplaceCart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    MarketplaceProduct product;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal quantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 2)
    BigDecimal unitPriceSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 50)
    String unitSnapshot;

    // Denormalized fields for inventory reservation (derived from product)
    @Column(name = "farmer_user_id", nullable = false)
    Long farmerUserId;

    @Column(name = "lot_id")
    Integer lotId;

    @Column(name = "lot_code", length = 120)
    String lotCode;

    @Column(name = "product_name_snapshot")
    String productNameSnapshot;

    @Column(name = "product_slug_snapshot")
    String productSlugSnapshot;

    @Column(name = "image_url_snapshot", length = 1024)
    String imageUrlSnapshot;

    @Column(name = "traceable_snapshot")
    Boolean traceableSnapshot;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // Initialize snapshot fields from product if not set.
        if (product != null) {
            if (lotId == null) {
                lotId = product.getLotId();
            }
            if (lotCode == null || lotCode.isBlank()) {
                lotCode = product.getLotCode();
            }
            if (farmerUserId == null) {
                farmerUserId = product.getFarmerUserId();
            }
            if (productNameSnapshot == null || productNameSnapshot.isBlank()) {
                productNameSnapshot = product.getName();
            }
            if (productSlugSnapshot == null || productSlugSnapshot.isBlank()) {
                productSlugSnapshot = product.getSlug();
            }
            if (imageUrlSnapshot == null || imageUrlSnapshot.isBlank()) {
                imageUrlSnapshot = product.getImageUrl();
            }
            if (traceableSnapshot == null) {
                traceableSnapshot = lotId != null;
            }
            if (unitSnapshot == null || unitSnapshot.isBlank()) {
                unitSnapshot = product.getUnit();
            }
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
