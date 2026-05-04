package org.example.QuanLyMuaVu.module.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RecordStockMovementRequest {

    @NotNull(message = "KEY_INVALID")
    Integer supplyLotId;

    @NotNull(message = "KEY_INVALID")
    Integer warehouseId;

    Integer locationId;

    @NotBlank(message = "KEY_INVALID")
    String movementType;

    @NotNull(message = "KEY_INVALID")
    @Min(value = 1, message = "KEY_INVALID")
    BigDecimal quantity;

    Integer seasonId;

    Integer taskId;

    String note;
}

