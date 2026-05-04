package org.example.QuanLyMuaVu.module.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request for recording stock movements by Admin.
 * Note: quantity can be negative for ADJUST movements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRecordMovementRequest {

    @NotNull(message = "Supply lot ID is required")
    Integer supplyLotId;

    @NotNull(message = "Warehouse ID is required")
    Integer warehouseId;

    Integer locationId; // Optional

    @NotBlank(message = "Movement type is required")
    String movementType; // IN, OUT, ADJUST

    @NotNull(message = "Quantity is required")
    BigDecimal quantity; // Can be negative for ADJUST

    Integer seasonId; // Optional

    String note;
}
