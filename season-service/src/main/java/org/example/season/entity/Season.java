package org.example.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.example.season.enums.SeasonStatus;

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

    @Column(name = "plot_id", nullable = false)
    Integer plotId;

    @Column(name = "crop_id", nullable = false)
    Integer cropId;

    @Column(name = "variety_id")
    Integer varietyId;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "planned_harvest_date")
    LocalDate plannedHarvestDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    SeasonStatus status = SeasonStatus.PLANNED;

    @Column(name = "initial_plant_count", nullable = false)
    Integer initialPlantCount;

    @Column(name = "current_plant_count")
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

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SeasonStatus.PLANNED;
        }
    }
}
