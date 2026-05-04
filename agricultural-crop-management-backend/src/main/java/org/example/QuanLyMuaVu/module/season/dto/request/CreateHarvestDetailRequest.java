package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CreateHarvestDetailRequest {

    @NotNull(message = "KEY_INVALID")
    LocalDate harvestDate;

    @NotNull(message = "INVALID_HARVEST_QUANTITY")
    @DecimalMin(value = "0.0", inclusive = false, message = "INVALID_HARVEST_QUANTITY")
    BigDecimal quantity;

    @NotNull(message = "KEY_INVALID")
    @DecimalMin(value = "0.0", inclusive = false, message = "KEY_INVALID")
    BigDecimal unit;

    @NotNull(message = "KEY_INVALID")
    Integer warehouseId;

    Integer locationId;

    Integer productId;

    @NotBlank(message = "KEY_INVALID")
    String productName;

    String productVariant;

    @NotBlank(message = "KEY_INVALID")
    String lotCode;

    String inventoryUnit;

    String grade;

    String note;
}
