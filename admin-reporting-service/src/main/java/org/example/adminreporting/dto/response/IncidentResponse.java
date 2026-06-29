package org.example.adminreporting.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
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
