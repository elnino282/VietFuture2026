package org.example.inventory.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReceiveHarvestRequest {
    Integer seasonId;
    String seasonName;
    Integer farmId;
    String farmName;
    Integer plotId;
    String plotName;
    Integer cropId;
    String cropName;
    Integer varietyId;
    String varietyName;
    LocalDate harvestDate;
    BigDecimal quantity;
    String grade;
    Integer warehouseId;
    Integer locationId;
    Integer productId;
    String productName;
    String productVariant;
    String lotCode;
    String unit;
    String note;
}
