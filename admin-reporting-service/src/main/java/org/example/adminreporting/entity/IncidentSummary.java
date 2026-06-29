package org.example.adminreporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_incident_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentSummary {
    @Id
    @Column(name = "incident_id")
    private Integer incidentId;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "incident_type")
    private String incidentType;

    @Column(name = "severity")
    private String severity;

    @Column(name = "resolved_at")
    private java.time.LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
}
