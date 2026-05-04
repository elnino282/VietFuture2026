package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainEventListenerTest {

    @Mock
    private IncidentCommandPort incidentCommandPort;

    @Mock
    private SeasonQueryPort seasonQueryPort;

    @InjectMocks
    private DomainEventListener domainEventListener;

    @Test
    @DisplayName("TASK_ASSIGNED creates assignee notification via event flow")
    void handleTaskAssigned_CreatesNotification() {
        User owner = User.builder().id(10L).build();
        User assignee = User.builder().id(20L).build();
        Farm farm = Farm.builder().id(5).user(owner).build();
        Plot plot = Plot.builder().id(6).farm(farm).build();
        Season season = Season.builder().id(7).plot(plot).build();
        Task task = Task.builder()
                .id(99)
                .title("Inspect irrigation")
                .season(season)
                .user(assignee)
                .build();

        domainEventListener.handleTaskAssigned(new TaskAssignedEvent(task, owner.getId()));

        verify(incidentCommandPort).createNotificationFromEvent(
                eq(20L),
                eq("Task assigned"),
                contains("Inspect irrigation"),
                eq("/seasons/7/tasks/99"));
    }

    @Test
    @DisplayName("INCIDENT_REPORTED creates notification and high-severity alert")
    void handleIncidentReported_CreatesNotificationAndAlert() {
        User owner = User.builder().id(10L).build();
        Farm farm = Farm.builder().id(5).user(owner).build();
        Plot plot = Plot.builder().id(6).farm(farm).build();
        Season season = Season.builder().id(7).plot(plot).build();
        Incident incident = Incident.builder()
                .id(11)
                .seasonId(7)
                .season(season)
                .incidentType("Pest outbreak")
                .severity(IncidentSeverity.HIGH)
                .reportedById(30L)
                .build();

        when(seasonQueryPort.findSeasonById(7)).thenReturn(Optional.of(season));

        domainEventListener.handleIncidentReported(new IncidentReportedEvent(incident));

        verify(incidentCommandPort).createNotificationFromEvent(
                eq(10L),
                eq("Incident reported: Pest outbreak"),
                eq("Severity HIGH incident requires follow-up."),
                eq("/seasons/7/incidents/11"));
        verify(incidentCommandPort).saveAlert(any(org.example.QuanLyMuaVu.module.incident.entity.Alert.class));
    }
}
