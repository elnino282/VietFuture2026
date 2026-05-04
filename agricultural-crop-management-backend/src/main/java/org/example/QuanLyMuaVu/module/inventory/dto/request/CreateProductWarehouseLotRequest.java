package org.example.QuanLyMuaVu.module.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class CreateProductWarehouseLotRequest {

    String lotCode;

    Integer productId;

    @NotBlank(message = "KEY_INVALID")
    String productName;

    String productVariant;

    Integer seasonId;

    @NotNull(message = "KEY_INVALID")
    Integer farmId;

    @NotNull(message = "KEY_INVALID")
    Integer plotId;

    Integer harvestId;

    @NotNull(message = "KEY_INVALID")
    Integer warehouseId;

    Integer locationId;

    @NotNull(message = "KEY_INVALID")
    LocalDate harvestedAt;

    LocalDateTime receivedAt;

    @NotBlank(message = "KEY_INVALID")
    String unit;

    @NotNull(message = "KEY_INVALID")
    @DecimalMin(value = "0.0", inclusive = false, message = "KEY_INVALID")
    BigDecimal initialQuantity;

    String grade;

    String qualityStatus;

    String traceabilityData;

    String note;

    String status;
}

