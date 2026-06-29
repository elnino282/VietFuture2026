package org.example.incident.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.incident.entity.Alert;
import org.example.incident.entity.Incident;
import org.example.incident.port.IncidentCommandPort;
import org.example.incident.repository.AlertRepository;
import org.example.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.incident.event.AlertChangedEvent;
import org.example.incident.event.DomainEventPublisher;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class IncidentCommandService implements IncidentCommandPort {

    AlertRepository alertRepository;
    IncidentRepository incidentRepository;
    ExternalServiceClient externalServiceClient;
    NotificationService notificationService;
    DomainEventPublisher domainEventPublisher;

    @Override
    public Alert saveAlert(Alert alert) {
        boolean isNew = (alert.getId() == null);
        Alert saved = alertRepository.save(alert);
        domainEventPublisher.publish(new AlertChangedEvent(saved, isNew ? AlertChangedEvent.Action.CREATED : AlertChangedEvent.Action.UPDATED));
        return saved;
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
        ExternalServiceClient.UserInternalDto user = externalServiceClient.getUser(userId);
        if (user != null) {
            notificationService.createNotification(user.getId(), title, message, link);
        }
    }

    @Override
    public void createNotificationFromEvent(Long userId, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        ExternalServiceClient.UserInternalDto user = externalServiceClient.getUser(userId);
        if (user != null) {
            notificationService.createNotificationFromEvent(user.getId(), title, message, link);
        }
    }
}
