package org.example.season.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDiaryEventDto {
    private LocalDate eventDate;
    private String eventType; // FERTILIZER | PESTICIDE | IRRIGATION | FIELD_LOG | HARVEST | SOIL_TEST
    private String title;
    private String description;
    private String sourceService;
    private Integer sourceId;
}

