package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceCartResponse(
        Long userId,
        List<MarketplaceCartItemResponse> items,
        List<MarketplaceCartSellerGroupResponse> sellerGroups,
        BigDecimal itemCount,
        BigDecimal subtotal,
        String currency) {
}
