package org.example.season.dto.request;

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
    Integer warehouseId;
    Integer locationId;
    Integer productId;
    String productName;
    String productVariant;
    String lotCode;
    String unit;
    String note;
}
