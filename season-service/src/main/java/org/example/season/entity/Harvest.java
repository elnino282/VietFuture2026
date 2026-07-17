package org.example.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
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
import org.example.season.enums.WarehouseReceiptStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "harvests")
public class Harvest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "harvest_id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "season_id")
    Season season;

    @Column(name = "harvest_date", nullable = false)
    LocalDate harvestDate;

    @Column(name = "quantity", nullable = false)
    BigDecimal quantity;

    @Column(name = "unit", nullable = false)
    BigDecimal unit;

    @Column(name = "grade", length = 20)
    String grade;

    String note;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "warehouse_received_date")
    LocalDate warehouseReceivedDate;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "warehouse_receipt_status")
    WarehouseReceiptStatus warehouseReceiptStatus;

    @Column(name = "gross_wet_weight")
    BigDecimal grossWetWeight;

    @Column(name = "net_dry_weight")
    BigDecimal netDryWeight;

    // === Quality Grading ===
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "quality_grade", length = 20)
    org.example.season.enums.QualityGrade qualityGrade;

    @Column(name = "quality_notes", columnDefinition = "TEXT")
    String qualityNotes;

    @Column(name = "sub_standard_quantity", precision = 19, scale = 3)
    BigDecimal subStandardQuantity;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "sub_standard_disposition", length = 30)
    org.example.season.enums.SubStandardDisposition subStandardDisposition;

    // === Packaging & Processing ===
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "packaging_type", length = 30)
    org.example.season.enums.PackagingType packagingType;

    @Column(name = "packaging_count")
    Integer packagingCount;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "processing_type", length = 30)
    org.example.season.enums.ProcessingType processingType;

    @Column(name = "crop_category", length = 30)
    String cropCategory;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
