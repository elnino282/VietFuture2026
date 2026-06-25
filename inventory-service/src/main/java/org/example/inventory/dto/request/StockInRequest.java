package org.example.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class StockInRequest {
    @NotNull(message = "Warehouse ID is required")
    Integer warehouseId;

    Integer locationId;

    @NotNull(message = "Supplier ID is required")
    Integer supplierId;

    @NotNull(message = "Supply item ID is required")
    Integer supplyItemId;

    String batchCode;
    String expiryDate;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    BigDecimal quantity;

    Boolean confirmRestricted;
    String note;
}
