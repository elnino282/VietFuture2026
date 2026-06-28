package org.example.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
@Table(name = "marketplace_order_items")
public class MarketplaceOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    MarketplaceOrder order;

    @Column(name = "product_id", nullable = false)
    Long productId;

    // Denormalized fields from product
    @Column(name = "farmer_user_id", nullable = false)
    Long farmerUserId;

    @Column(name = "product_name_snapshot", nullable = false)
    String productNameSnapshot;

    @Column(name = "product_slug_snapshot", nullable = false, length = 191)
    String productSlugSnapshot;

    @Column(name = "image_url_snapshot", length = 1024)
    String imageUrlSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 2)
    BigDecimal unitPriceSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 50)
    String unitSnapshot;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal quantity;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    BigDecimal lineTotal;

    @Column(name = "traceable_snapshot", nullable = false)
    Boolean traceableSnapshot;

    // Denormalized fields for traceability
    @Column(name = "farm_id")
    Integer farmId;

    @Column(name = "season_id")
    Integer seasonId;

    @Column(name = "lot_id")
    Integer lotId;

    @Column(name = "lot_code", length = 120)
    String lotCode;

    @Column(name = "lot_warehouse_name", length = 255)
    String lotWarehouseName;

    @Column(name = "lot_storage_location", length = 255)
    String lotStorageLocation;

    @Column(name = "lot_harvest_date")
    LocalDateTime lotHarvestDate;

    @Column(name = "lot_received_at")
    LocalDateTime lotReceivedAt;

    @Column(name = "lot_grade", length = 50)
    String lotGrade;

    @Column(name = "lot_initial_quantity", precision = 19, scale = 3)
    BigDecimal lotInitialQuantity;

    @Column(name = "plot_id")
    Integer plotId;

    @Column(name = "plot_name", length = 255)
    String plotName;

    @Column(name = "plot_area", precision = 19, scale = 4)
    BigDecimal plotArea;

    @Column(name = "crop_name", length = 255)
    String cropName;

    @Column(name = "farm_name", length = 255)
    String farmName;

    @Column(name = "season_name", length = 255)
    String seasonName;

    @Column(name = "published_at_snapshot")
    LocalDateTime publishedAtSnapshot;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
