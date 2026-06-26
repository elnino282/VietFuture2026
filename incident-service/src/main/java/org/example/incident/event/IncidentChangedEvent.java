package org.example.incident.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import org.example.incident.entity.Incident;

@Getter
public class IncidentChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        RESOLVED,
        CANCELLED
    }

    private final Action action;
    private final Integer incidentId;
    private final Integer seasonId;
    private final Integer farmId;
    private final Long reporterUserId;
    private final String incidentType;
    private final String severity;
    private final String description;
    private final String status;
    private final LocalDate deadline;
    private final LocalDateTime resolvedAt;

    public IncidentChangedEvent(Incident incident, Action action, Long reporterUserId) {
        super("Incident",
              incident != null && incident.getId() != null ? incident.getId().toString() : "unknown",
              "incident-service",
              "incident.event.incident." + action.name().toLowerCase());
        this.action = action;
        this.incidentId = incident != null ? incident.getId() : null;
        this.seasonId = incident != null ? incident.getSeasonId() : null;
        this.farmId = incident != null ? incident.getFarmId() : null;
        this.reporterUserId = reporterUserId;
        this.incidentType = incident != null ? incident.getIncidentType() : null;
        this.severity = incident != null && incident.getSeverity() != null ? incident.getSeverity().name() : null;
        this.description = incident != null ? incident.getDescription() : null;
        this.status = incident != null && incident.getStatus() != null ? incident.getStatus().name() : null;
        this.deadline = incident != null ? incident.getDeadline() : null;
        this.resolvedAt = incident != null ? incident.getResolvedAt() : null;
    }
}
