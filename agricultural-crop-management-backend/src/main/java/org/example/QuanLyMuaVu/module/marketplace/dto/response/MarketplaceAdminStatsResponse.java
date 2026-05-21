package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MarketplaceAdminStatsResponse(
        Long totalProducts,
        Long pendingReviewProducts,
        Long publishedProducts,
        Long hiddenProducts,
        Long totalOrders,
        Long activeOrders,
        Long completedOrders,
        Long cancelledOrders,
        Long pendingPaymentVerificationOrders,
        BigDecimal totalRevenue,
        boolean hasProducts,
        boolean hasOrders,
        boolean hasRevenueData,
        LocalDateTime lastOrderAt,
        List<String> unavailableReasons) {
}
