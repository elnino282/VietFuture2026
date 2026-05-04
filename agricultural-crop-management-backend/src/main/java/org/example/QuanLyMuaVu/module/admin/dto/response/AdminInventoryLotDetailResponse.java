package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.util.List;
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
public class AdminInventoryLotDetailResponse {
    Integer lotId;
    Integer itemId;
    String itemName;
    String lotCode;
    String unit;
    String supplierName;
    String expiryDate;
    String status;
    Double onHandTotal;
    List<BalanceRow> balances;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BalanceRow {
        Integer warehouseId;
        String warehouseName;
        Integer farmId;
        String farmName;
        Integer locationId;
        String locationLabel;
        Double quantity;
    }
}
