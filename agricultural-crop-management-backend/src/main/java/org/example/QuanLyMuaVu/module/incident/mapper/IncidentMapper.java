package org.example.QuanLyMuaVu.module.incident.mapper;

import jakarta.persistence.EntityNotFoundException;
import java.util.function.Supplier;
import org.example.QuanLyMuaVu.module.incident.dto.response.IncidentResponse;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public IncidentResponse toResponse(Incident incident) {
        if (incident == null) {
            return null;
        }
        Integer resolvedSeasonId = incident.getSeasonId() != null
                ? incident.getSeasonId()
                : safeGet(() -> incident.getSeason() != null ? incident.getSeason().getId() : null);
        String resolvedSeasonName = safeGet(
                () -> incident.getSeason() != null ? incident.getSeason().getSeasonName() : null);
        Long resolvedReportedById = incident.getReportedById() != null
                ? incident.getReportedById()
                : safeGet(() -> incident.getReportedBy() != null ? incident.getReportedBy().getId() : null);
        String resolvedReportedByUsername = safeGet(
                () -> incident.getReportedBy() != null ? incident.getReportedBy().getUsername() : null);

        return IncidentResponse.builder()
                .incidentId(incident.getId())
                .seasonId(resolvedSeasonId)
                .seasonName(resolvedSeasonName)
                .reportedById(resolvedReportedById)
                .reportedByUsername(resolvedReportedByUsername)
                .incidentType(incident.getIncidentType())
                .severity(incident.getSeverity() != null ? incident.getSeverity().name() : null)
                .description(incident.getDescription())
                .status(incident.getStatus() != null ? incident.getStatus().name() : null)
                .deadline(incident.getDeadline())
                .resolvedAt(incident.getResolvedAt())
                .createdAt(incident.getCreatedAt())
                .build();
    }

    private <T> T safeGet(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (LazyInitializationException | EntityNotFoundException ex) {
            return null;
        }
    }
}
