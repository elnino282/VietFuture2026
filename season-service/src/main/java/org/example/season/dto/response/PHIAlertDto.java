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
public class PHIAlertDto {
    private Integer seasonId;
    private String seasonName;
    private String pesticideName;
    private LocalDate appliedDate;
    private Integer requiredIntervalDays;
    private LocalDate earliestSafeDate;
    private Long daysRemaining;
}

