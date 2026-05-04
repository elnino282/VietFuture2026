package org.example.QuanLyMuaVu.module.admin.entity;

import jakarta.persistence.*;
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
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    Integer id;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "url", nullable = false, length = 1000)
    String url;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "crop", length = 50)
    String crop;

    @Column(name = "stage", length = 50)
    String stage;

    @Column(name = "topic", length = 50)
    String topic;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    Boolean isPublic = true;

    @Column(name = "created_by")
    Long createdBy;

    @Column(name = "document_type", length = 50)
    String documentType;

    @Builder.Default
    @Column(name = "view_count")
    Integer viewCount = 0;

    @Builder.Default
    @Column(name = "is_pinned")
    Boolean isPinned = false;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
