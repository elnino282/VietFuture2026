package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record MarketplaceMergeCartRequest(
        @NotEmpty List<@Valid MarketplaceMergeCartItem> items) {

    public record MarketplaceMergeCartItem(
            @NotNull Long productId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity) {
    }
}
