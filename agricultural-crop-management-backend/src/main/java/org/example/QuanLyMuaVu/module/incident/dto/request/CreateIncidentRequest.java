package org.example.QuanLyMuaVu.module.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateIncidentRequest {

    @NotNull(message = "Season ID is required")
    Integer seasonId;

    @NotBlank(message = "Incident type is required")
    String incidentType;

    @NotBlank(message = "Severity is required")
    String severity;

    @NotBlank(message = "Description is required")
    String description;

    LocalDate deadline;
}
