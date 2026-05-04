package org.example.QuanLyMuaVu.module.farm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "farms")
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_id")
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    org.example.QuanLyMuaVu.module.identity.entity.User user;

    @Column(name = "farm_name", nullable = false)
    String name;

    @ManyToOne
    @JoinColumn(name = "province_id", nullable = false)
    Province province;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    Ward ward;

    @Column(name = "area")
    BigDecimal area;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    Double averageRating = 0.0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    Integer ratingCount = 0;

    /**
     * Soft-delete flag. Active farms have active = true.
     */
    @Column(name = "active", nullable = false)
    Boolean active;
}
