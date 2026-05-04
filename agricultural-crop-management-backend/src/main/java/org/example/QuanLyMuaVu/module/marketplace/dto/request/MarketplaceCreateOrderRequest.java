package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;

public record MarketplaceCreateOrderRequest(
        @NotNull MarketplacePaymentMethod paymentMethod,
        Long addressId,
        String shippingRecipientName,
        String shippingPhone,
        String shippingAddressLine,
        String note,
        String idempotencyKey) {
}
