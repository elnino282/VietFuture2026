package org.example.QuanLyMuaVu.module.inventory.dto.response;

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

    // Supplier info
    Integer supplierId;
    String supplierName;

    // Supply item info
    Integer supplyItemId;
    String supplyItemName;
    String unit;
    Boolean restrictedFlag;
}
