package org.example.QuanLyMuaVu.module.incident.port;

import org.example.QuanLyMuaVu.module.incident.entity.Alert;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;

public interface IncidentCommandPort {

    Alert saveAlert(Alert alert);

    Incident saveIncident(Incident incident);

    void createNotification(Long userId, String title, String message, String link);

    void createNotificationFromEvent(Long userId, String title, String message, String link);
}
