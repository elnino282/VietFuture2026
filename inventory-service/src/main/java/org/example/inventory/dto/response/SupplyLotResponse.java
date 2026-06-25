package org.example.inventory.dto.response;

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
public class SupplyLotResponse {
    Integer id;
    String batchCode;
    LocalDate expiryDate;
    String status;
    Integer supplierId;
    String supplierName;
    Integer supplyItemId;
    String supplyItemName;
    String unit;
    Boolean restrictedFlag;
}
