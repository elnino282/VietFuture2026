package org.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
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
@Table(name = "inventory_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "supply_lot_id", "warehouse_id", "location_id" })
})
public class InventoryBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "supply_lot_id", nullable = false)
    Integer supplyLotId;

    @Column(name = "warehouse_id", nullable = false)
    Integer warehouseId;

    @Column(name = "location_id")
    Integer locationId;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    BigDecimal quantity;
}
