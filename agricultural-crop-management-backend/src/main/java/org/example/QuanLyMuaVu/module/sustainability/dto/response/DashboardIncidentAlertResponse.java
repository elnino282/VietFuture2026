package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class DashboardIncidentAlertResponse {
    String id;
    String type;
    String severity;
    String title;
    String description;
    Integer seasonId;
    Integer plotId;
    LocalDateTime createdAt;
    LocalDate dueDate;
    String actionUrl;
    String actionTarget;
}
