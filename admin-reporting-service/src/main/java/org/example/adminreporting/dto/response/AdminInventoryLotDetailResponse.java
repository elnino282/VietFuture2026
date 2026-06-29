package org.example.adminreporting.dto.response;

import java.util.List;
import lombok.*;
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
