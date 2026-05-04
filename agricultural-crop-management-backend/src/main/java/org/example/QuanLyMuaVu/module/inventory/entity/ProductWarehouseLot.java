package org.example.QuanLyMuaVu.module.inventory.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "product_warehouse_lots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_warehouse_lot_code", columnNames = "lot_code"),
        @UniqueConstraint(name = "uk_product_warehouse_harvest", columnNames = "harvest_id")
})
public class ProductWarehouseLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "lot_code", nullable = false, length = 100)
    String lotCode;

    @Column(name = "product_id")
    Integer productId;

    @Column(name = "product_name", nullable = false, length = 255)
    String productName;

    @Column(name = "product_variant", length = 255)
    String productVariant;

    @ManyToOne
    @JoinColumn(name = "season_id")
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @ManyToOne
    @JoinColumn(name = "farm_id", nullable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Farm farm;

    @ManyToOne
    @JoinColumn(name = "plot_id", nullable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Plot plot;

    @ManyToOne
    @JoinColumn(name = "harvest_id")
    org.example.QuanLyMuaVu.module.season.entity.Harvest harvest;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "location_id")
    StockLocation location;

    @Column(name = "harvested_at", nullable = false)
    LocalDate harvestedAt;

    @Column(name = "received_at", nullable = false)
    LocalDateTime receivedAt;

    @Column(name = "unit", nullable = false, length = 30)
    String unit;

    @Column(name = "initial_quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal initialQuantity;

    @Column(name = "on_hand_quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal onHandQuantity;

    @Column(name = "grade", length = 50)
    String grade;

    @Column(name = "quality_status", length = 50)
    String qualityStatus;

    @Column(name = "traceability_data", columnDefinition = "TEXT")
    String traceabilityData;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    ProductWarehouseLotStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    org.example.QuanLyMuaVu.module.identity.entity.User createdBy;

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
        if (status == null) {
            status = ProductWarehouseLotStatus.IN_STOCK;
        }
        if (onHandQuantity == null) {
            onHandQuantity = initialQuantity;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

