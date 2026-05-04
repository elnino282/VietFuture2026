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
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "nutrient_input_events")
public class NutrientInputEvent {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "input_source", nullable = false, length = 40)
    NutrientInputSource inputSource;

    @Column(name = "n_kg", nullable = false, precision = 19, scale = 4)
    BigDecimal nKg;

    @Column(name = "applied_date")
    LocalDate appliedDate;

    @Column(name = "measured", nullable = false)
    @Builder.Default
    Boolean measured = Boolean.TRUE;

    @Column(name = "data_source", length = 120)
    String dataSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 40)
    NutrientInputSourceType sourceType;

    @Column(name = "source_document", length = 255)
    String sourceDocument;

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
