package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "task_progress_logs", indexes = {
        @Index(name = "idx_task_progress_task", columnList = "task_id"),
        @Index(name = "idx_task_progress_employee", columnList = "employee_user_id"),
        @Index(name = "idx_task_progress_logged_at", columnList = "logged_at")
})
public class TaskProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    Task task;

    @ManyToOne
    @JoinColumn(name = "employee_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User employee;

    @Column(name = "progress_percent", nullable = false)
    Integer progressPercent;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "evidence_url", length = 1000)
    String evidenceUrl;

    @Column(name = "logged_at", nullable = false)
    LocalDateTime loggedAt;

    @PrePersist
    void onCreate() {
        if (loggedAt == null) {
            loggedAt = LocalDateTime.now();
        }
    }
}

