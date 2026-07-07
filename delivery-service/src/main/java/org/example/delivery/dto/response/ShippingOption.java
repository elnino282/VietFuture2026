package org.example.delivery.dto.response;

import java.math.BigDecimal;

public record ShippingOption(
    String type,          // "standard" | "same_day"
    Integer providerId,
    String providerName,
    BigDecimal shippingFeeVnd,
    int estimatedHours,
    boolean isSameDay,
    boolean isColdChain
) {}
