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
public class TaskResponse {
    Integer taskId;
    String title;
    String description;
    String status;
    LocalDate plannedDate;
    LocalDate dueDate;
    LocalDate actualStartDate;
    LocalDate actualEndDate;
    String notes;
    Integer seasonId;
    String seasonName;
    Long userId;
    String userName;
    LocalDateTime createdAt;
}
