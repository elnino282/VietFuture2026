package org.example.QuanLyMuaVu.module.sustainability.dto.response;

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
public class DashboardRecentActivityResponse {
    String id;
    String type;
    String title;
    String description;
    LocalDateTime occurredAt;
    String actorName;
    String entityType;
    String entityId;
    String actionUrl;
}
