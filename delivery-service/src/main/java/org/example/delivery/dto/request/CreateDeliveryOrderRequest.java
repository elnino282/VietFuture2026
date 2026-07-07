package org.example.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDeliveryOrderRequest(
    @NotNull Long marketplaceOrderId,
    @NotNull Integer providerId,
    @NotNull BigDecimal shippingFeeVnd,
    boolean isPerishable,
    boolean requiresColdChain,
    @NotBlank String recipientName,
    @NotBlank String recipientPhone,
    @NotBlank String recipientAddress,
    @NotBlank String recipientProvince,
    @NotNull BigDecimal weightKg
) {}
