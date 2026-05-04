package org.example.QuanLyMuaVu.module.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "marketplace_products")
public class MarketplaceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Version
    @Column(name = "version")
    Long version;

    @Column(name = "slug", nullable = false, length = 191, unique = true)
    String slug;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "category", length = 120)
    String category;

    @Column(name = "short_description", length = 500)
    String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    BigDecimal price;

    @Column(name = "unit", nullable = false, length = 50)
    String unit;

    @Column(name = "stock_quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal stockQuantity;

    @Column(name = "image_url", length = 1024)
    String imageUrl;

    @Column(name = "image_urls_json", columnDefinition = "TEXT")
    String imageUrlsJson;

    @ManyToOne
    @JoinColumn(name = "farmer_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User farmerUser;

    @ManyToOne
    @JoinColumn(name = "farm_id")
    org.example.QuanLyMuaVu.module.farm.entity.Farm farm;

    @ManyToOne
    @JoinColumn(name = "season_id")
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot lot;

    @Column(name = "traceable", nullable = false)
    @Builder.Default
    Boolean traceable = Boolean.TRUE;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    Double averageRating = 0.0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    Integer ratingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    MarketplaceProductStatus status = MarketplaceProductStatus.DRAFT;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
