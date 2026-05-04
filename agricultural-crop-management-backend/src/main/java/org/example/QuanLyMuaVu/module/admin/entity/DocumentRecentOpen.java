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
@Table(name = "document_recent_opens")
public class DocumentRecentOpen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "document_id", nullable = false)
    Integer documentId;

    @Column(name = "opened_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime openedAt;

    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
    }
}
