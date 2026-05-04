package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;

public record MarketplaceCartItemResponse(
        Long productId,
        String slug,
        String name,
        String imageUrl,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal maxQuantity,
        Long farmerUserId,
        Boolean traceable) {
}
