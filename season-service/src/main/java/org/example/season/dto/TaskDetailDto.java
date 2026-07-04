package org.example.season.dto;

import lombok.Builder;
import lombok.Data;
import org.example.season.enums.TaskStatus;
import java.time.LocalDate;

@Data
@Builder
public class TaskDetailDto {
    private Long taskId;
    private String taskName;
    private Integer estimatedDays;
    private LocalDate estimatedCompletionDate;
    private TaskStatus status;
    private Long plotId;
    private String plotName;
    private java.math.BigDecimal plotArea;
    private Long workTeamId;
}
