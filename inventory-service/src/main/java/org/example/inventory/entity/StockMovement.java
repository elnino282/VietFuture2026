package org.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.example.inventory.enums.StockMovementType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "supply_lot_id", nullable = false)
    Integer supplyLotId;

    @Column(name = "warehouse_id", nullable = false)
    Integer warehouseId;

    @Column(name = "location_id")
    Integer locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    StockMovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    BigDecimal quantity;

    @Column(name = "movement_date", nullable = false)
    LocalDateTime movementDate;

    @Column(name = "season_id")
    Integer seasonId;

    @Column(name = "task_id")
    Integer taskId;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;
}
