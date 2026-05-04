package org.example.QuanLyMuaVu.module.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class OnHandRowResponse {

    Integer warehouseId;
    String warehouseName;
    Integer locationId;
    String locationLabel;
    Integer supplyLotId;
    String batchCode;
    String supplyItemName;
    String unit;
    LocalDate expiryDate;
    String lotStatus;
    BigDecimal onHandQuantity;
}
