package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = \"certification_audits\")
public class CertificationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = \"record_id\", nullable = false)
    Integer recordId;

    @Column(name = \"audit_type\", nullable = false, length = 30)
    String auditType;

    @Column(name = \"scheduled_date\")
    LocalDate scheduledDate;

    @Column(name = \"auditor_user_id\")
    Long auditorUserId;

    @Column(name = \"auditor_org_name\")
    String auditorOrgName;

    @Column(length = 30, nullable = false)
    @Builder.Default
    String status = \"SCHEDULED\";

    @Column(name = \"interview_notes\", columnDefinition = \"TEXT\")
    String interviewNotes;

    @Column(name = \"sample_collection_notes\", columnDefinition = \"TEXT\")
    String sampleCollectionNotes;

    @Column(name = \"conducted_at\")
    LocalDateTime conductedAt;

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

