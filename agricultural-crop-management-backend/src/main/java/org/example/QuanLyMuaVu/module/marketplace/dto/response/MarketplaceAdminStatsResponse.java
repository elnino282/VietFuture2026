package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;

public record MarketplaceAdminStatsResponse(
        Long totalProducts,
        Long pendingReviewProducts,
        Long publishedProducts,
        Long hiddenProducts,
        Long totalOrders,
        Long activeOrders,
        Long completedOrders,
        Long cancelledOrders,
        BigDecimal totalRevenue) {
}
