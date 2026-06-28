package org.example.marketplace.dto.client;

public record FarmSummaryDto(
        Integer id,
        String name,
        String provinceName,
        String wardName,
        java.math.BigDecimal area) {
}
