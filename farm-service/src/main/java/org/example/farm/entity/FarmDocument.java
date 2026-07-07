package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "farm_documents")
public class FarmDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "farm_id", nullable = false)
    Integer farmId;

    @Column(name = "document_type", nullable = false, length = 50)
    String documentType;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "file_url", length = 1000)
    String fileUrl;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "verification_status", length = 20)
    @Builder.Default
    String verificationStatus = "PENDING";

    @Column(name = "verified_by")
    Long verifiedBy;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "created_by", nullable = false)
    Long createdBy;

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
