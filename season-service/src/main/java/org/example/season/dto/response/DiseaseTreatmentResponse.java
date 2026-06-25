package org.example.season.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class DiseaseTreatmentResponse {

    Integer id;
    Integer diseaseRecordId;

    LocalDateTime treatedAt;
    String method;

    Integer supplyItemId;
    String supplyItemName;

    Integer supplyLotId;
    String batchCode;

    String materialName;
    BigDecimal quantityUsed;
    String unit;
    BigDecimal costAmount;

    Integer expenseId;
    String effectiveness;
    String resultSummary;
    LocalDate nextReviewAt;
    String notes;

    Long createdByUserId;
    String createdByUsername;
    String createdByDisplayName;
    String createdByType;
    Boolean canEdit;
    Boolean canDelete;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
