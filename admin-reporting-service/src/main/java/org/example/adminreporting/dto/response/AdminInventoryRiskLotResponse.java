package org.example.adminreporting.dto.response;

import lombok.*;
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
