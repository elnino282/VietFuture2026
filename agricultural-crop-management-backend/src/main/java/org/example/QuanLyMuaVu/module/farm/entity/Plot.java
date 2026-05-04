package org.example.QuanLyMuaVu.module.farm.entity;

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
@Table(name = "plots")
public class Plot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plot_id")
    Integer id;

    /**
     * Reference to plot creator (created_by in DDL).
     */
    @ManyToOne
    @JoinColumn(name = "created_by")
    org.example.QuanLyMuaVu.module.identity.entity.User user;

    /**
     * Link to a farm for grouping plots.
     */
    @ManyToOne
    @JoinColumn(name = "farm_id", nullable = false)
    Farm farm;

    @Column(name = "plot_name")
    String plotName;

    BigDecimal area;

    /**
     * Soil type as VARCHAR(50) per DDL schema.
     */
    @Column(name = "soil_type", length = 50)
    String soilType;

    @Column(name = "boundary_geojson", columnDefinition = "LONGTEXT")
    String boundaryGeoJson;

    /**
     * Status as VARCHAR(30) per DDL schema. Default: IN_USE.
     */
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    String status = "IN_USE";

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
