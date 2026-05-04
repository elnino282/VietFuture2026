package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
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
public class HarvestStockContextResponse {

    Integer warehouseId;
    String warehouseName;
    String productName;
    String lotCode;
    Long matchingLots;
    BigDecimal onHandQuantity;
    String unit;
}
