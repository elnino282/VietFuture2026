package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;

/**
 * Observer Pattern: org.example.QuanLyMuaVu.module.incident.entity.Incident Reported Event.
 * <p>
 * Published when a new incident (pest outbreak, disease, weather damage) is
 * reported.
 * Listeners can use this to:
 * - Auto-create mitigation tasks for HIGH severity incidents
 * - Send alerts to farm managers
 * - Trigger AI assistant to suggest remediation
 */
@Getter
public class IncidentReportedEvent extends DomainEvent {

    private final Integer incidentId;
    private final String incidentType;
    private final String severity;
    private final Integer seasonId;
    private final Integer farmId;
    private final Integer plotId;
    private final Integer cropId;
    private final Long ownerUserId;
    private final Long reportedByUserId;

    public IncidentReportedEvent(org.example.QuanLyMuaVu.module.incident.entity.Incident incident) {
        super("org.example.QuanLyMuaVu.module.incident.entity.Incident", incident.getId() != null ? incident.getId().toString() : "unknown");
        this.incidentId = incident.getId();
        this.incidentType = incident.getIncidentType();
        this.severity = incident.getSeverity() != null ? incident.getSeverity().name() : null;
        this.seasonId = incident.getSeasonId() != null
                ? incident.getSeasonId()
                : incident.getSeason() != null ? incident.getSeason().getId() : null;
        this.farmId = incident.getSeason() != null
                && incident.getSeason().getPlot() != null
                && incident.getSeason().getPlot().getFarm() != null
                        ? incident.getSeason().getPlot().getFarm().getId()
                        : null;
        this.plotId = incident.getSeason() != null && incident.getSeason().getPlot() != null
                ? incident.getSeason().getPlot().getId()
                : null;
        this.cropId = incident.getSeason() != null && incident.getSeason().getCrop() != null
                ? incident.getSeason().getCrop().getId()
                : null;
        this.ownerUserId = incident.getSeason() != null
                && incident.getSeason().getPlot() != null
                && incident.getSeason().getPlot().getFarm() != null
                && incident.getSeason().getPlot().getFarm().getUser() != null
                        ? incident.getSeason().getPlot().getFarm().getUser().getId()
                        : null;
        this.reportedByUserId = incident.getReportedById() != null
                ? incident.getReportedById()
                : incident.getReportedBy() != null ? incident.getReportedBy().getId() : null;
    }

    @Override
    public String getEventType() {
        return "INCIDENT_REPORTED";
    }
}
