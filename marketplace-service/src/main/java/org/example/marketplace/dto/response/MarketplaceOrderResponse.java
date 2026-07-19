package org.example.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.marketplace.model.MarketplaceOrderStatus;

public record MarketplaceOrderResponse(
        Long id,
        String orderCode,
        String orderGroupCode,
        Long buyerUserId,
        Long farmerUserId,
        MarketplaceOrderStatus status,
        MarketplaceOrderPaymentResponse payment,
        String shippingRecipientName,
        String shippingPhone,
        String shippingAddressLine,
        String note,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Boolean canCancel,
        Boolean isPreOrder,
        java.time.LocalDate requestedDeliveryDate,
        java.time.LocalDate harvestReadyDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MarketplaceOrderItemResponse> items) {
}
