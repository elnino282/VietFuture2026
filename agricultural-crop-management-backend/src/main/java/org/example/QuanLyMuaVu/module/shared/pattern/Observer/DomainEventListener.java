package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventListener {

    private static final String ALERT_STATUS_OPEN = "OPEN";
    private static final String ALERT_TYPE_INCIDENT = "INCIDENT";
    private static final String ALERT_ACTION_TYPE = "MITIGATION";

    private final IncidentCommandPort incidentCommandPort;
    private final SeasonQueryPort seasonQueryPort;

    @EventListener
    @Async
    public void handleSeasonCreated(SeasonCreatedEvent event) {
        log.info("[EVENT] org.example.QuanLyMuaVu.module.season.entity.Season created: id={}, name={}, plotId={}, cropId={}, ownerId={}",
                event.getSeasonId(),
                event.getSeasonName(),
                event.getPlotId(),
                event.getCropId(),
                event.getOwnerUserId());

        String link = event.getSeasonId() != null ? "/seasons/" + event.getSeasonId() : "/seasons";
        incidentCommandPort.createNotificationFromEvent(
                event.getOwnerUserId(),
                "New season created",
                "org.example.QuanLyMuaVu.module.season.entity.Season '" + safeText(event.getSeasonName(), "N/A") + "' is ready for planning.",
                link);
    }

    @EventListener
    @Async
    public void handleTaskCompleted(TaskCompletedEvent event) {
        log.info("[EVENT] Task completed: id={}, title={}, seasonId={}, previousStatus={}, assigneeId={}, ownerId={}",
                event.getTaskId(),
                event.getTaskTitle(),
                event.getSeasonId(),
                event.getPreviousStatus(),
                event.getAssigneeUserId(),
                event.getOwnerUserId());

        String link = event.getSeasonId() != null
                ? "/seasons/" + event.getSeasonId() + "/tasks/" + event.getTaskId()
                : "/tasks/" + event.getTaskId();
        String message = "Task '" + safeText(event.getTaskTitle(), "N/A") + "' has been completed.";

        incidentCommandPort.createNotificationFromEvent(
                event.getOwnerUserId(),
                "Task completed",
                message,
                link);

        if (event.getAssigneeUserId() != null && !event.getAssigneeUserId().equals(event.getOwnerUserId())) {
            incidentCommandPort.createNotificationFromEvent(
                    event.getAssigneeUserId(),
                    "Task completion confirmed",
                    message,
                    link);
        }
    }

    @EventListener
    @Async
    public void handleTaskAssigned(TaskAssignedEvent event) {
        log.info("[EVENT] Task assigned: id={}, title={}, seasonId={}, assigneeId={}, ownerId={}, assignedBy={}",
                event.getTaskId(),
                event.getTaskTitle(),
                event.getSeasonId(),
                event.getAssigneeUserId(),
                event.getOwnerUserId(),
                event.getAssignedByUserId());

        if (event.getAssigneeUserId() == null) {
            return;
        }

        String link = event.getSeasonId() != null
                ? "/seasons/" + event.getSeasonId() + "/tasks/" + event.getTaskId()
                : "/tasks/" + event.getTaskId();

        incidentCommandPort.createNotificationFromEvent(
                event.getAssigneeUserId(),
                "Task assigned",
                "Task '" + safeText(event.getTaskTitle(), "N/A") + "' has been assigned to you.",
                link);
    }

    @EventListener
    @Async
    public void handleIncidentReported(IncidentReportedEvent event) {
        log.info("[EVENT] Incident reported: id={}, type={}, severity={}, seasonId={}, by user={}",
                event.getIncidentId(),
                event.getIncidentType(),
                event.getSeverity(),
                event.getSeasonId(),
                event.getReportedByUserId());

        String severity = safeText(event.getSeverity(), "UNKNOWN");
        String title = "Incident reported: " + safeText(event.getIncidentType(), "General");
        String link = event.getSeasonId() != null
                ? "/seasons/" + event.getSeasonId() + "/incidents/" + event.getIncidentId()
                : "/incidents/" + event.getIncidentId();
        String message = "Severity " + severity + " incident requires follow-up.";

        incidentCommandPort.createNotificationFromEvent(
                event.getOwnerUserId(),
                title,
                message,
                link);

        if (isHighSeverity(severity)) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = event.getSeasonId() != null
                    ? seasonQueryPort.findSeasonById(event.getSeasonId()).orElse(null)
                    : null;

            org.example.QuanLyMuaVu.module.incident.entity.Alert alert = org.example.QuanLyMuaVu.module.incident.entity.Alert.builder()
                    .type(ALERT_TYPE_INCIDENT)
                    .severity(severity.toUpperCase())
                    .status(ALERT_STATUS_OPEN)
                    .farmId(event.getFarmId() != null
                            ? event.getFarmId()
                            : season != null && season.getPlot() != null && season.getPlot().getFarm() != null
                                    ? season.getPlot().getFarm().getId()
                                    : null)
                    .farm(season != null && season.getPlot() != null ? season.getPlot().getFarm() : null)
                    .seasonId(event.getSeasonId())
                    .season(season)
                    .plotId(event.getPlotId() != null
                            ? event.getPlotId()
                            : season != null && season.getPlot() != null ? season.getPlot().getId() : null)
                    .plot(season != null ? season.getPlot() : null)
                    .cropId(event.getCropId() != null
                            ? event.getCropId()
                            : season != null && season.getCrop() != null ? season.getCrop().getId() : null)
                    .crop(season != null ? season.getCrop() : null)
                    .title("High severity incident: " + safeText(event.getIncidentType(), "General"))
                    .message("Incident #" + event.getIncidentId() + " marked HIGH severity. Immediate mitigation required.")
                    .suggestedActionType(ALERT_ACTION_TYPE)
                    .suggestedActionUrl(link)
                    .recipientFarmerIds(event.getOwnerUserId() != null ? String.valueOf(event.getOwnerUserId()) : null)
                    .createdAt(LocalDateTime.now())
                    .build();

            incidentCommandPort.saveAlert(alert);
        }
    }

    @EventListener
    @Async
    public void handleExpenseChanged(ExpenseChangedEvent event) {
        log.info("[EVENT] Expense changed: action={}, id={}, seasonId={}, amount={}",
                event.getAction(),
                event.getExpenseId(),
                event.getSeasonId(),
                event.getAmount());

        String action = event.getAction() != null ? event.getAction().name().toLowerCase() : "updated";
        String link = event.getSeasonId() != null
                ? "/seasons/" + event.getSeasonId() + "/expenses"
                : "/expenses";
        String category = safeText(event.getCategory(), "Uncategorized");
        incidentCommandPort.createNotificationFromEvent(
                event.getOwnerUserId(),
                "Expense " + action,
                "Expense " + action + " for category '" + category + "'.",
                link);
    }

    @EventListener
    @Async
    public void handleHarvestChanged(HarvestChangedEvent event) {
        log.info("[EVENT] Harvest changed: action={}, id={}, seasonId={}, quantity={}",
                event.getAction(),
                event.getHarvestId(),
                event.getSeasonId(),
                event.getQuantity());

        String action = event.getAction() != null ? event.getAction().name().toLowerCase() : "updated";
        String link = event.getSeasonId() != null
                ? "/seasons/" + event.getSeasonId() + "/harvests"
                : "/harvests";
        incidentCommandPort.createNotificationFromEvent(
                event.getOwnerUserId(),
                "Harvest " + action,
                "Harvest lot " + action + " successfully.",
                link);
    }

    private boolean isHighSeverity(String severity) {
        String normalized = safeText(severity, "").toUpperCase();
        return "HIGH".equals(normalized) || "CRITICAL".equals(normalized);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
