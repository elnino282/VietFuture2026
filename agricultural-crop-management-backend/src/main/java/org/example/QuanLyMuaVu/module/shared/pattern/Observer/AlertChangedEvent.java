package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;
import org.example.QuanLyMuaVu.module.incident.entity.Alert;

@Getter
public class AlertChangedEvent extends DomainEvent {

    private final Integer alertId;
    private final Integer seasonId;
    private final String alertType;
    private final String severity;
    private final String status;

    public AlertChangedEvent(Alert alert) {
        super("Alert", alert.getId() != null ? alert.getId().toString() : "unknown");
        this.alertId = alert.getId();
        this.seasonId = alert.getSeasonId();
        this.alertType = alert.getType();
        this.severity = alert.getSeverity();
        this.status = alert.getStatus();
    }

    @Override
    public String getEventType() {
        return "ALERT_CHANGED";
    }
}
