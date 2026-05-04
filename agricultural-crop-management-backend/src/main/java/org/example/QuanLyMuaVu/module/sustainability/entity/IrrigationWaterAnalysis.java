package org.example.QuanLyMuaVu.module.sustainability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "irrigation_water_analyses")
public class IrrigationWaterAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "season_id", nullable = false)
    Integer seasonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false, insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @Column(name = "plot_id", nullable = false)
    Integer plotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false, insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Plot plot;

    @Column(name = "sample_date", nullable = false)
    LocalDate sampleDate;

    @Column(name = "nitrate_mg_per_l", precision = 19, scale = 4)
    BigDecimal nitrateMgPerL;

    @Column(name = "ammonium_mg_per_l", precision = 19, scale = 4)
    BigDecimal ammoniumMgPerL;

    @Column(name = "total_n_mg_per_l", precision = 19, scale = 4)
    BigDecimal totalNmgPerL;

    @Column(name = "irrigation_volume_m3", nullable = false, precision = 19, scale = 4)
    BigDecimal irrigationVolumeM3;

    @Column(name = "legacy_n_contribution_kg", precision = 19, scale = 4)
    BigDecimal legacyNContributionKg;

    @Column(name = "legacy_event_id")
    Integer legacyEventId;

    @Column(name = "legacy_derived", nullable = false)
    @Builder.Default
    Boolean legacyDerived = Boolean.FALSE;

    @Column(name = "measured", nullable = false)
    @Builder.Default
    Boolean measured = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 40)
    NutrientInputSourceType sourceType;

    @Column(name = "source_document", length = 255)
    String sourceDocument;

    @Column(name = "lab_reference", length = 255)
    String labReference;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "created_by_user_id")
    Long createdByUserId;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
