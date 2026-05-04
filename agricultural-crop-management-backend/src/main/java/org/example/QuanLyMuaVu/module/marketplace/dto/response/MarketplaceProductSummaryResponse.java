package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;

public record MarketplaceProductSummaryResponse(
        Long id,
        String slug,
        String name,
        String category,
        String shortDescription,
        BigDecimal price,
        String unit,
        BigDecimal stockQuantity,
        BigDecimal availableQuantity,
        String imageUrl,
        Long farmerUserId,
        String farmerDisplayName,
        Integer farmId,
        String farmName,
        Integer seasonId,
        String seasonName,
        Integer lotId,
        String region,
        Boolean traceable,
        Double ratingAverage,
        Long ratingCount,
        MarketplaceProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
