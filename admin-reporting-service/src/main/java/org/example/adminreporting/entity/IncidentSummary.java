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
}
