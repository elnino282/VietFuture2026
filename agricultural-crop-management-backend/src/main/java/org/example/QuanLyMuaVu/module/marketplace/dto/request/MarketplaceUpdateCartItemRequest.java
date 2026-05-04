package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MarketplaceUpdateCartItemRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity) {
}
