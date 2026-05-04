package org.example.QuanLyMuaVu.module.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class StockMovementResponse {

    Integer id;
    Integer supplyLotId;
    String batchCode;
    String supplyItemName;
    String unit;
    Integer warehouseId;
    String warehouseName;
    Integer locationId;
    String locationLabel;
    String movementType;
    BigDecimal quantity;
    LocalDateTime movementDate;
    Integer seasonId;
    String seasonName;
    Integer taskId;
    String taskTitle;
    String note;
}
