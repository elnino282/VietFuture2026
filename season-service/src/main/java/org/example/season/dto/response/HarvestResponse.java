package org.example.season.dto.response;

import java.math.BigDecimal;
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
public class HarvestResponse {
    Integer id;
    Integer seasonId;
    String seasonName;
    LocalDate harvestDate;
    BigDecimal quantity;
    BigDecimal unit;
    String grade;
    BigDecimal revenue;
    String note;
    String status;
    LocalDateTime createdAt;

    // === Quality Grading ===
    String qualityGrade;
    String qualityNotes;
    BigDecimal subStandardQuantity;
    String subStandardDisposition;

    // === Packaging & Processing ===
    String packagingType;
    Integer packagingCount;
    String processingType;

    // === Crop Info ===
    String cropCategory;
    BigDecimal grossWetWeight;
    BigDecimal netDryWeight;
    String warehouseReceiptStatus;
    Integer postHarvestDelayDays;
}
