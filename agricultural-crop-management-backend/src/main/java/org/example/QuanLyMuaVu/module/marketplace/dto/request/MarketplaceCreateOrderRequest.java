package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceCreateOrderRequest(
        @NotNull MarketplacePaymentMethod paymentMethod,
        Long addressId,
        String shippingRecipientName,
        String shippingPhone,
        String shippingAddressLine,
        String note,
        String idempotencyKey,
        List<MarketplaceOrderItemRequest> items) {

    public MarketplaceCreateOrderRequest(
            MarketplacePaymentMethod paymentMethod,
            Long addressId,
            String shippingRecipientName,
            String shippingPhone,
            String shippingAddressLine,
            String note,
            String idempotencyKey) {
        this(paymentMethod, addressId, shippingRecipientName, shippingPhone, shippingAddressLine, note, idempotencyKey, null);
    }

    public record MarketplaceOrderItemRequest(
            @NotNull Long productId,
            @NotNull BigDecimal quantity) {
    }
}
