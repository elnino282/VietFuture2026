package org.example.QuanLyMuaVu.module.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "farm_id", nullable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Farm farm;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "type", length = 20)
    String type;

    @ManyToOne
    @JoinColumn(name = "province_id")
    org.example.QuanLyMuaVu.module.farm.entity.Province province;

    @ManyToOne
    @JoinColumn(name = "ward_id")
    org.example.QuanLyMuaVu.module.farm.entity.Ward ward;
}

