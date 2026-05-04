package org.example.QuanLyMuaVu.module.admin.dto.response;

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
public class AdminInventoryRiskLotResponse {
    Integer lotId;
    Integer itemId;
    String itemName;
    String lotCode;
    Integer farmId;
    String farmName;
    String expiryDate;
    Double onHand;
    Long daysToExpiry;
    String status;
    String severity;
    String unit;
    Double unitCost;
}
