package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MarketplaceUpdateReviewRequest(
        @Min(1) @Max(5) Integer rating,
        String comment) {
}
