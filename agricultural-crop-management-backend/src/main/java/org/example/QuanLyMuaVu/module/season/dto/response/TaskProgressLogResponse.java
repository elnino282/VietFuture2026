package org.example.QuanLyMuaVu.module.season.dto.response;

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
public class TaskProgressLogResponse {
    Integer id;
    Integer taskId;
    String taskTitle;
    Integer seasonId;
    String seasonName;
    Long employeeUserId;
    String employeeName;
    Integer progressPercent;
    String note;
    String evidenceUrl;
    LocalDateTime loggedAt;
}

