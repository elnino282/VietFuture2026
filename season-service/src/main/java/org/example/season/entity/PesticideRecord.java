package org.example.season.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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
@Table(name = "pesticide_records")
public class PesticideRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "season_id", nullable = false)
    Integer seasonId;

    @Column(name = "plot_id", nullable = false)
    Integer plotId;

    @Column(name = "field_log_id")
    Integer fieldLogId;

    @Column(name = "pesticide_name", nullable = false)
    String pesticideName;

    @Column(name = "active_ingredient")
    String activeIngredient;

    @Column(name = "phi_days", nullable = false)
    Integer phiDays;

    @Column(
        name = "harvest_allowed_date",
        insertable = false,
        updatable = false,
        columnDefinition = "DATE GENERATED ALWAYS AS (application_date + phi_days)"
    )
    LocalDate harvestAllowedDate;

    @Column(name = "application_date", nullable = false)
    LocalDate applicationDate;

    @Column(name = "application_method")
    String applicationMethod;

    String dosage;

    @Column(name = "target_pest")
    String targetPest;

    String note;

    @Column(name = "created_by", nullable = false)
    Long createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
