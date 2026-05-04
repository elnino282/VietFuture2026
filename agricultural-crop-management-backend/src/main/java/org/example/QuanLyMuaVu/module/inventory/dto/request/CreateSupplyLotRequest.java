package org.example.QuanLyMuaVu.module.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
public class CreateSupplyLotRequest {

    @NotNull(message = "Supply item ID is required")
    Integer supplyItemId;

    Integer supplierId;

    @Size(max = 100, message = "Batch code must not exceed 100 characters")
    String batchCode;

    LocalDate expiryDate;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    String status;
}
