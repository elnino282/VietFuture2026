package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.time.LocalDateTime;

public record MarketplaceReviewResponse(
        Long id,
        Long productId,
        Long orderId,
        Long orderItemId,
        Long buyerUserId,
        String buyerDisplayName,
        Integer rating,
        String comment,
        Boolean hidden,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
