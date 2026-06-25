package org.example.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.season.enums.DiseaseSeverity;
import org.example.season.enums.DiseaseStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "disease_records")
public class DiseaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disease_record_id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    Season season;

    @Column(name = "plot_id")
    Integer plotId;

    @Column(name = "crop_id")
    Integer cropId;

    @Column(name = "variety_id")
    Integer varietyId;

    @Column(name = "reported_by_user_id", nullable = false)
    Long reportedByUserId;

    @Column(name = "incident_id")
    Integer incidentId;

    @Column(name = "disease_name", nullable = false, length = 150)
    String diseaseName;

    @Column(name = "symptom_summary", columnDefinition = "TEXT")
    String symptomSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    DiseaseSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    DiseaseStatus status;

    @Column(name = "detected_at", nullable = false)
    LocalDateTime detectedAt;

    @Column(name = "affected_plant_count")
    Integer affectedPlantCount;

    @Column(name = "affected_area_value", precision = 14, scale = 3)
    BigDecimal affectedAreaValue;

    @Column(name = "affected_area_unit", length = 20)
    String affectedAreaUnit;

    @Column(name = "evidence_url", length = 1000)
    String evidenceUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
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
