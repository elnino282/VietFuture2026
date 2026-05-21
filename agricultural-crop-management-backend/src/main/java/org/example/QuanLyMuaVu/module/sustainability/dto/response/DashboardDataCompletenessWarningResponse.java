package org.example.QuanLyMuaVu.module.sustainability.dto.response;

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
public class DashboardDataCompletenessWarningResponse {
    String warningId;
    String title;
    String source;
    String type;
    String status;
    LocalDate dueDate;
    String actionTarget;
    Integer seasonId;
    String inputCode;
}
