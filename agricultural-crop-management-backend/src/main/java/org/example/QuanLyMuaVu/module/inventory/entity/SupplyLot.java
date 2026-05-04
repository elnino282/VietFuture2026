package org.example.QuanLyMuaVu.module.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "supply_lots")
public class SupplyLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "supply_item_id", nullable = false)
    SupplyItem supplyItem;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    Supplier supplier;

    @Column(name = "batch_code", length = 100)
    String batchCode;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "status", length = 20)
    String status;
}

