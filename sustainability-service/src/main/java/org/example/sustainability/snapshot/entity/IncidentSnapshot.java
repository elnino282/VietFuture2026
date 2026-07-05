package org.example.sustainability.snapshot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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
@Table(name = "incident_snapshots")
public class IncidentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "incident_id")
    Integer incidentId;

    @Column(name = "season_id")
    Integer seasonId;

    @Column(name = "farm_id")
    Integer farmId;

    @Column(name = "reported_by_id")
    Long reportedById;

    @Column(name = "incident_type")
    String incidentType;

    @Column(name = "severity")
    String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "status")
    String status;

    @Column(name = "deadline")
    LocalDate deadline;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @Column(name = "snapshot_at")
    LocalDateTime snapshotAt;
}
