package org.example.QuanLyMuaVu.module.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

/**
 * Request DTO for triaging an incident (OPEN -> IN_PROGRESS).
 */
@Data
public class TriageIncidentRequest {

    @NotBlank(message = "Severity is required")
    private String severity;

    private LocalDate deadline;

    private Long assigneeId;
}
