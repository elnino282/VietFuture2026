package org.example.adminreporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_alert_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSummary {
    @Id
    @Column(name = "alert_id")
    private Integer alertId;

    @Column(name = "season_id")
    private Integer seasonId;

    @Column(name = "type")
    private String type;

    @Column(name = "severity")
    private String severity;

    @Column(name = "status")
    private String status;
}
