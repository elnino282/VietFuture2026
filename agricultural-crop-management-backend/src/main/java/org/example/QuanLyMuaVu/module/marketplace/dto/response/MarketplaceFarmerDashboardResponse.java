package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceFarmerDashboardResponse(
        Long totalProducts,
        Long pendingReviewProducts,
        Long publishedProducts,
        Long lowStockProducts,
        Long pendingOrders,
        BigDecimal totalRevenue,
        List<MarketplaceOrderResponse> recentOrders) {
}
