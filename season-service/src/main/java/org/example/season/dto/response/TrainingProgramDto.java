package org.example.season.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingProgramDto {
    private Integer id;
    private String title;
    private String category;
    private String description;
    private Boolean isMandatory;
    private LocalDateTime createdAt;
}

