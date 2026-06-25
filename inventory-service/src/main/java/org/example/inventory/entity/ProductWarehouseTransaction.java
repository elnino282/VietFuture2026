package org.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.example.inventory.enums.ProductWarehouseTransactionType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "product_warehouse_transactions")
public class ProductWarehouseTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "lot_id", nullable = false)
    Integer lotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    ProductWarehouseTransactionType transactionType;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 30)
    String unit;

    @Column(name = "resulting_on_hand", nullable = false, precision = 19, scale = 3)
    BigDecimal resultingOnHand;

    @Column(name = "reference_type", length = 50)
    String referenceType;

    @Column(name = "reference_id", length = 100)
    String referenceId;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "created_by")
    Long createdBy;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
