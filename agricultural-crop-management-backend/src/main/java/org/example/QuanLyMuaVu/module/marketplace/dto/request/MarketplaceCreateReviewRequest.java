package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MarketplaceCreateReviewRequest(
        @NotNull Long orderItemId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment) {
}
