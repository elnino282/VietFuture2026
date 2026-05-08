package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
import org.example.QuanLyMuaVu.Enums.TreatmentEffectiveness;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "disease_treatments")
public class DiseaseTreatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disease_treatment_id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "disease_record_id", nullable = false)
    DiseaseRecord diseaseRecord;

    @Column(name = "treated_at", nullable = false)
    LocalDateTime treatedAt;

    @Column(name = "method", nullable = false, length = 100)
    String method;

    @Column(name = "supply_item_id")
    Integer supplyItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_item_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem supplyItem;

    @Column(name = "supply_lot_id")
    Integer supplyLotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_lot_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot supplyLot;

    @Column(name = "material_name", length = 150)
    String materialName;

    @Column(name = "quantity_used", precision = 14, scale = 3)
    BigDecimal quantityUsed;

    @Column(name = "unit", length = 20)
    String unit;

    @Column(name = "cost_amount", precision = 19, scale = 2)
    BigDecimal costAmount;

    @Column(name = "expense_id")
    Integer expenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.financial.entity.Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(name = "effectiveness", length = 20)
    TreatmentEffectiveness effectiveness;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    String resultSummary;

    @Column(name = "next_review_at")
    LocalDate nextReviewAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "created_by_user_id", nullable = false)
    Long createdByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false, insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
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
