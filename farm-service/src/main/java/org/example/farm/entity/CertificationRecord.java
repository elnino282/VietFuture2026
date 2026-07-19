package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "certification_records")
public class CertificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "farm_id", nullable = false)
    Integer farmId;

    @Column(name = "standard_id", nullable = false)
    Integer standardId;

    @Column(name = "compliance_score", precision = 5, scale = 2)
    BigDecimal complianceScore;

    @Column(length = 20)
    @Builder.Default
    String status = "IN_PROGRESS";

    @Column(name = "applied_at")
    LocalDateTime appliedAt;

    @Column(name = "certified_at")
    LocalDateTime certifiedAt;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "auditor_notes", columnDefinition = "TEXT")
    String auditorNotes;

    @Column(name = "certificate_number", length = 100)
    String certificateNumber;

    @Column(name = "certificate_document_id")
    Integer certificateDocumentId;

    @Column(name = "next_periodic_review_date")
    LocalDate nextPeriodicReviewDate;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "published_by_user_id")
    Long publishedByUserId;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
