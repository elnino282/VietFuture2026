package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
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
public class DiseaseRecordResponse {

    Integer id;
    Integer seasonId;
    String seasonName;

    Integer plotId;
    String plotName;

    Integer cropId;
    String cropName;

    Integer varietyId;
    String varietyName;

    Long reportedByUserId;
    String reportedByUsername;
    String reportedByDisplayName;
    String reportedByType;
    Boolean canEdit;
    Boolean canDelete;

    Integer incidentId;

    String diseaseName;
    String symptomSummary;
    String severity;
    String status;
    LocalDateTime detectedAt;

    Integer affectedPlantCount;
    BigDecimal affectedAreaValue;
    String affectedAreaUnit;

    String evidenceUrl;
    String notes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    Long treatmentCount;
    LocalDateTime latestTreatmentAt;
}
