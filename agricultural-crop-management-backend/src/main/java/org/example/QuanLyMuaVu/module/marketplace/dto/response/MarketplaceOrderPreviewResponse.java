package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Preview of what a checkout will produce — grouped by seller,
 * with totals calculated but nothing persisted.
 */
public record MarketplaceOrderPreviewResponse(
        List<SellerGroup> sellerGroups,
        BigDecimal grandSubtotal,
        BigDecimal grandShippingFee,
        BigDecimal grandTotal,
        String shippingRecipientName,
        String shippingPhone,
        String shippingAddressLine,
        int totalSellerOrders,
        String currency) {

    public record SellerGroup(
            Long farmerUserId,
            String farmerDisplayName,
            Integer farmId,
            String farmName,
            List<PreviewItem> items,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal groupTotal) {
    }

    public record PreviewItem(
            Long productId,
            String slug,
            String name,
            String imageUrl,
            BigDecimal unitPrice,
            BigDecimal quantity,
            BigDecimal lineTotal,
            boolean traceable) {
    }
}
