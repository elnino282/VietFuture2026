package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MarketplaceFarmerDashboardResponse(
        Long totalProducts,
        Long pendingReviewProducts,
        Long publishedProducts,
        Long lowStockProducts,
        Long pendingOrders,
        BigDecimal totalRevenue,
        boolean hasProducts,
        boolean hasOrders,
        boolean hasRevenueData,
        LocalDateTime lastOrderAt,
        List<String> unavailableReasons,
        List<MarketplaceOrderResponse> recentOrders) {
}
