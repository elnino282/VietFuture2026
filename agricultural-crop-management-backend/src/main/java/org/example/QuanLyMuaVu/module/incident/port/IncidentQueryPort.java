package org.example.QuanLyMuaVu.module.incident.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.incident.entity.Alert;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentQueryPort {

    List<Incident> findAllIncidentsBySeasonId(Integer seasonId);

    Page<Incident> findAllIncidents(Pageable pageable);

    Optional<Incident> findIncidentById(Integer incidentId);

    long countOpenIncidentsBySeasonId(Integer seasonId);

    long countOpenIncidentsByOwnerId(Long ownerId);

    Page<Alert> searchAlerts(
            String type,
            String severity,
            String status,
            Integer farmId,
            LocalDateTime fromDate,
            Pageable pageable);

    Optional<Alert> findAlertById(Integer alertId);
}
