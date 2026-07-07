package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "certification_item_statuses")
public class CertificationItemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "record_id", nullable = false)
    Integer recordId;

    @Column(name = "checklist_item_id", nullable = false)
    Integer checklistItemId;

    @Column(length = 20)
    @Builder.Default
    String status = "PENDING";

    @Column(name = "evidence_url", length = 1000)
    String evidenceUrl;

    @Column(columnDefinition = "TEXT")
    String notes;

    @Column(name = "checked_at")
    LocalDateTime checkedAt;

    @Column(name = "checked_by")
    Long checkedBy;

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
