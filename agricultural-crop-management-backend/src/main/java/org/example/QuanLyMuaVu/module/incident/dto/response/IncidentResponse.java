package org.example.QuanLyMuaVu.module.incident.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncidentResponse {

    Integer incidentId;
    Integer seasonId;
    String seasonName;
    Long reportedById;
    String reportedByUsername;
    String incidentType;
    String severity;
    String description;
    String status;
    LocalDate deadline;
    LocalDateTime resolvedAt;
    LocalDateTime createdAt;
}
