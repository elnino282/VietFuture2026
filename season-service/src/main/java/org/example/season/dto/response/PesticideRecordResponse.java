package org.example.season.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PesticideRecordResponse {
    private Integer id;
    private Integer seasonId;
    private Integer plotId;
    private Integer fieldLogId;
    private String pesticideName;
    private String activeIngredient;
    private Integer phiDays;
    private LocalDate harvestAllowedDate;
    private LocalDate applicationDate;
    private String applicationMethod;
    private String dosage;
    private String targetPest;
    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
