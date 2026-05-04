package org.example.QuanLyMuaVu.module.incident.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.incident.entity.Alert;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.incident.repository.AlertRepository;
import org.example.QuanLyMuaVu.module.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class IncidentCommandService implements IncidentCommandPort {

    AlertRepository alertRepository;
    IncidentRepository incidentRepository;
    IdentityQueryPort identityQueryPort;
    NotificationService notificationService;

    @Override
    public Alert saveAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    @Override
    public Incident saveIncident(Incident incident) {
        return incidentRepository.save(incident);
    }

    @Override
    public void createNotification(Long userId, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        identityQueryPort.findUserById(userId)
                .ifPresent(user -> notificationService.createNotification(user.getId(), title, message, link));
    }

    @Override
    public void createNotificationFromEvent(Long userId, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        identityQueryPort.findUserById(userId)
                .ifPresent(user -> notificationService.createNotificationFromEvent(user.getId(), title, message, link));
    }
}
