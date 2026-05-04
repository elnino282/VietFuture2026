package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * Read-only dashboard task view combining task, plot, and assignee data.
 */
@Entity
@Immutable
@Subselect(
        "SELECT " +
                "t.task_id AS task_id, " +
                "t.title AS title, " +
                "t.description AS description, " +
                "t.planned_date AS planned_date, " +
                "t.due_date AS due_date, " +
                "t.status AS status, " +
                "t.season_id AS season_id, " +
                "t.user_id AS user_id, " +
                "u.full_name AS assignee_name, " +
                "p.plot_id AS plot_id, " +
                "p.plot_name AS plot_name " +
        "FROM tasks t " +
        "LEFT JOIN users u ON t.user_id = u.user_id " +
        "LEFT JOIN seasons s ON t.season_id = s.season_id " +
        "LEFT JOIN plots p ON s.plot_id = p.plot_id"
)
@Synchronize({ "tasks", "users", "seasons", "plots" })
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardTaskView {
    @Id
    @Column(name = "task_id")
    Integer taskId;

    @Column(name = "title")
    String title;

    @Column(name = "description")
    String description;

    @Column(name = "planned_date")
    LocalDate plannedDate;

    @Column(name = "due_date")
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    TaskStatus status;

    @Column(name = "season_id")
    Integer seasonId;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "assignee_name")
    String assigneeName;

    @Column(name = "plot_id")
    Integer plotId;

    @Column(name = "plot_name")
    String plotName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", insertable = false, updatable = false)
    Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Plot plot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User assignee;
}
