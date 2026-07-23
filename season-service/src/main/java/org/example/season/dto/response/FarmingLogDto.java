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
public class FarmingLogDto {
    private String id;
    private String seasonId;
    private LocalDate date;
    private String activityType; // FERTILIZER, PESTICIDE, WATERING, HARVEST, OTHER
    private String description;
    private Double amount;
    private String unit;
    private String performedBy;
    private String status; // COMPLETED, PENDING, CANCELLED
    private String notes;
    private String materialName;
    private Integer quarantineDays;
    private String soilCondition;
    private Boolean vietGapCompliant;
    private String vietGapReason;
}
