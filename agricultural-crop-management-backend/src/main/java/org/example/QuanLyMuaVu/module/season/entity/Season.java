package org.example.QuanLyMuaVu.module.season.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "seasons")
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id")
    Integer id;

    @Column(name = "season_name")
    String seasonName;

    @ManyToOne
    @JoinColumn(name = "plot_id", nullable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Plot plot;

    @ManyToOne
    @JoinColumn(name = "crop_id", nullable = false)
    org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop;

    @ManyToOne
    @JoinColumn(name = "variety_id")
    org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    /**
     * Planned harvest date for this season, used for scheduling and task planning.
     */
    @Column(name = "planned_harvest_date")
    LocalDate plannedHarvestDate;

    /**
     * Optional end date of the season.
     */
    @Column(name = "end_date")
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    SeasonStatus status;

    @Column(name = "initial_plant_count", nullable = false)
    Integer initialPlantCount;

    Integer currentPlantCount;

    @Column(name = "expected_yield_kg")
    BigDecimal expectedYieldKg;

    @Column(name = "actual_yield_kg")
    BigDecimal actualYieldKg;

    @Column(name = "budget_amount")
    BigDecimal budgetAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;
}
