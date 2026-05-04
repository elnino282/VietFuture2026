package org.example.QuanLyMuaVu.module.inventory.dto.request;

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
public class AdjustProductWarehouseLotRequest {

    @NotNull(message = "KEY_INVALID")
    BigDecimal quantityDelta;

    String note;
}

