package org.example.QuanLyMuaVu.module.marketplace.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MarketplaceOrderStatusConverter implements AttributeConverter<MarketplaceOrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(MarketplaceOrderStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public MarketplaceOrderStatus convertToEntityAttribute(String dbData) {
        return MarketplaceOrderStatus.fromStorageValue(dbData);
    }
}
