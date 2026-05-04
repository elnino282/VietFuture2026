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
import org.example.QuanLyMuaVu.Enums.StockMovementType;

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

    @ManyToOne
    @JoinColumn(name = "supply_lot_id", nullable = false)
    SupplyLot supplyLot;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "location_id")
    StockLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    StockMovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    BigDecimal quantity;

    @Column(name = "movement_date", nullable = false)
    LocalDateTime movementDate;

    @ManyToOne
    @JoinColumn(name = "season_id")
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @ManyToOne
    @JoinColumn(name = "task_id")
    org.example.QuanLyMuaVu.module.season.entity.Task task;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;
}

