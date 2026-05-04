package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.util.List;

public record MarketplaceCreateOrderResultResponse(
        String orderGroupCode,
        Integer splitCount,
        List<MarketplaceOrderResponse> orders) {
}
