package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.TaskStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User user;

    /**
     * Optional link to a season. New season operations APIs will always set this;
     * legacy APIs may leave it null.
     */
    @ManyToOne
    @JoinColumn(name = "season_id")
    Season season;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    /**
     * Planned date of the task within the season.
     */
    @Column(name = "planned_date")
    LocalDate plannedDate;

    @Column(name = "due_date")
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    TaskStatus status;

    @Column(name = "actual_start_date")
    LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    LocalDate actualEndDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;
}
