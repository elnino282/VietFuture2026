package org.example.season.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTrainingRecordDto {
    private Integer id;
    private Long userId;
    private Integer workTeamId;
    private TrainingProgramDto trainingProgram;
    private LocalDate trainedAt;
    private String trainerName;
    private List<String> evidenceUrls;
    private LocalDate certifiedUntil;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

