package org.example.marketplace.dto.client;

public record FarmDetailDto(
        Integer id,
        String name,
        String provinceName,
        String wardName,
        java.math.BigDecimal area,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        Double averageRating,
        Long userId) {
}
