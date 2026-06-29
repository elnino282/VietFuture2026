package org.example.adminreporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_task_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSummary {
    @Id
    @Column(name = "task_id")
    private Integer taskId;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @Column(name = "status", nullable = false)
    private String status;
}
