package org.example.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.Enumerated;

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

    @Column(name = "farm_id", nullable = false)
    Integer farmId;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "type", length = 20)
    String type;

    @Column(name = "province_id")
    Integer provinceId;

    @Column(name = "ward_id")
    Integer wardId;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "storage_category")
    org.example.inventory.enums.StorageCategory storageCategory;

    @Column(name = "temperature_min")
    java.math.BigDecimal temperatureMin;

    @Column(name = "temperature_max")
    java.math.BigDecimal temperatureMax;

    @Column(name = "humidity_min")
    java.math.BigDecimal humidityMin;

    @Column(name = "humidity_max")
    java.math.BigDecimal humidityMax;
}
