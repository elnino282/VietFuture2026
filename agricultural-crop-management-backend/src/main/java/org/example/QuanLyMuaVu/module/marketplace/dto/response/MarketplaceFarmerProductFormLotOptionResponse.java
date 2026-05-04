package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;

public record MarketplaceFarmerProductFormLotOptionResponse(
        Integer id,
        String lotCode,
        Integer farmId,
        String farmName,
        Integer seasonId,
        String seasonName,
        BigDecimal availableQuantity,
        LocalDate harvestedAt,
        String unit,
        String productName,
        String productVariant,
        Long linkedProductId,
        MarketplaceProductStatus linkedProductStatus) {
}
