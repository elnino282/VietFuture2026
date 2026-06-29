package org.example.incident.event;

import java.time.LocalDateTime;
import lombok.Getter;
import org.example.incident.entity.Alert;

@Getter
public class AlertChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED
    }

    private final Action action;
    private final Integer alertId;
    private final Integer seasonId;
    private final String type;
    private final String severity;
    private final String status;
    private final LocalDateTime createdAt;

    public AlertChangedEvent(Alert alert, Action action) {
        super("Alert",
              alert != null && alert.getId() != null ? alert.getId().toString() : "unknown",
              "incident-service",
              "incident.event.alert." + action.name().toLowerCase());
        this.action = action;
        this.alertId = alert != null ? alert.getId() : null;
        this.seasonId = alert != null ? alert.getSeasonId() : null;
        this.type = alert != null ? alert.getType() : null;
        this.severity = alert != null ? alert.getSeverity() : null;
        this.status = alert != null ? alert.getStatus() : null;
        this.createdAt = alert != null ? alert.getCreatedAt() : null;
    }
}
