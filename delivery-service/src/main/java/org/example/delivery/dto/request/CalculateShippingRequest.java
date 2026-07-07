package org.example.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CalculateShippingRequest(
    @NotBlank(message = "Sender province is required")
    String senderProvince,

    @NotBlank(message = "Recipient province is required")
    String recipientProvince,

    @NotNull(message = "Weight in kg is required")
    BigDecimal weightKg,

    boolean requiresColdChain,
    boolean prefersSameDay
) {}
