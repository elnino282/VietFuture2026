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
@Table(name = "certification_nonconformities")
public class CertificationNonconformity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "audit_id", nullable = false)
    Long auditId;

    @Column(name = "checklist_item_id")
    Integer checklistItemId;

    @Column(nullable = false, length = 20)
    String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(length = 30, nullable = false)
    @Builder.Default
    String status = "OPEN";

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
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

