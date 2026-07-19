package org.example.cropcatalog.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "crops")
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crop_id")
    Integer id;

    @Column(name = "crop_name")
    String cropName;

    @Column(name = "description")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    CropCategory category;

    @Column(name = "post_harvest_delay_days")
    Integer postHarvestDelayDays;

    @Column(name = "shelf_life_days")
    Integer shelfLifeDays;

    @Column(name = "default_storage_category")
    String defaultStorageCategory;

    @Column(name = "requires_cold_chain")
    Boolean requiresColdChain;
}
