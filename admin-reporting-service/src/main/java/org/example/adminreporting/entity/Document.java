package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "admin_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "crop", length = 50)
    private String crop;

    @Column(name = "stage", length = 50)
    private String stage;

    @Column(name = "topic", length = 50)
    private String topic;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
