package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = \"certification_corrective_actions\")
public class CertificationCorrectiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = \"nonconformity_id\", nullable = false)
    Long nonconformityId;

    @Column(name = \"plan_description\", columnDefinition = \"TEXT\")
    String planDescription;

    @Column(name = \"evidence_urls\", columnDefinition = \"TEXT\")
    String evidenceUrls;

    @Column(name = \"applies_from_season_id\")
    Integer appliesFromSeasonId;

    @Column(name = \"submitted_by_user_id\")
    Long submittedByUserId;

    @Column(name = \"submitted_at\")
    LocalDateTime submittedAt;

    @Column(name = \"reviewed_by_user_id\")
    Long reviewedByUserId;

    @Column(name = \"review_result\", length = 20)
    String reviewResult;

    @Column(name = \"review_note\", columnDefinition = \"TEXT\")
    String reviewNote;

    @Column(name = \"reviewed_at\")
    LocalDateTime reviewedAt;

    @Column(name = \"created_at\")
    LocalDateTime createdAt;

    @Column(name = \"updated_at\")
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

