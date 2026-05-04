package org.example.QuanLyMuaVu.module.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for resolving an incident (IN_PROGRESS -> RESOLVED).
 */
@Data
public class ResolveIncidentRequest {

    @NotBlank(message = "Resolution note is required")
    private String resolutionNote;
}
