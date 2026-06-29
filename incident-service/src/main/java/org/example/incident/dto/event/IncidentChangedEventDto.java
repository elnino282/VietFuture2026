package org.example.incident.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentChangedEventDto {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String producer;
    private String action;
    private Integer incidentId;
    private Integer seasonId;
    private Integer farmId;
    private Long reporterUserId;
    private String incidentType;
    private String severity;
    private String status;
    private java.time.LocalDate deadline;
    private java.time.LocalDateTime resolvedAt;
    private java.time.LocalDateTime createdAt;
}

