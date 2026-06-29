package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "admin_inventory_lot_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLotSummary {
    @Id
    @Column(name = "lot_id")
    private Integer lotId;

    @Column(name = "farm_id", nullable = false)
    private Integer farmId;

    @Column(name = "farm_name", nullable = false)
    private String farmName;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "quantity_on_hand")
    private java.math.BigDecimal quantityOnHand;
}
