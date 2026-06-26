package org.example.sustainability.snapshot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "farm_snapshots")
public class FarmSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "farm_id")
    Integer farmId;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "farm_name")
    String farmName;

    @Column(name = "province_id")
    Integer provinceId;

    @Column(name = "province_name")
    String provinceName;

    @Column(name = "ward_id")
    Integer wardId;

    @Column(name = "ward_name")
    String wardName;

    @Column(name = "area", precision = 19, scale = 4)
    BigDecimal area;

    @Column(name = "latitude", precision = 10, scale = 6)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 6)
    BigDecimal longitude;

    @Column(name = "active")
    Boolean active;

    @Column(name = "snapshot_at")
    LocalDateTime snapshotAt;
}
