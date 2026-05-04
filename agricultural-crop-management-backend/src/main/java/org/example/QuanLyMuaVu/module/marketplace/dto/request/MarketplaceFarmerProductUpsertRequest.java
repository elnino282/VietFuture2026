package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record MarketplaceFarmerProductUpsertRequest(
        @NotBlank String name,
        String category,
        String shortDescription,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal stockQuantity,
        String imageUrl,
        List<String> imageUrls,
        @NotNull Integer lotId) {
}
