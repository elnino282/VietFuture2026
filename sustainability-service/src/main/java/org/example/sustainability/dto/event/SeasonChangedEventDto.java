package org.example.sustainability.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonChangedEventDto {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String producer;
    private Integer seasonId;
    private String seasonName;
    private Integer plotId;
    private Integer cropId;
    private Integer farmId;
    private Integer varietyId;
    private LocalDate startDate;
    private LocalDate plannedHarvestDate;
    private LocalDate endDate;
    private String status;
    private Integer initialPlantCount;
    private Integer currentPlantCount;
    private BigDecimal expectedYieldKg;
    private BigDecimal actualYieldKg;
    private BigDecimal budgetAmount;
    private String notes;
}
