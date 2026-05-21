package org.example.QuanLyMuaVu.module.marketplace.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MarketplaceOrderStatusConverterTest {

    private final MarketplaceOrderStatusConverter converter = new MarketplaceOrderStatusConverter();

    @Test
    void convertToEntityAttribute_mapsLegacyPendingToPendingPayment() {
        MarketplaceOrderStatus result = converter.convertToEntityAttribute("PENDING");
        assertEquals(MarketplaceOrderStatus.PENDING_PAYMENT, result);
    }

    @Test
    void convertToEntityAttribute_mapsLegacyDeliveringToShipped() {
        MarketplaceOrderStatus result = converter.convertToEntityAttribute("DELIVERING");
        assertEquals(MarketplaceOrderStatus.SHIPPED, result);
    }

    @Test
    void convertToEntityAttribute_mapsCanonicalStatus() {
        MarketplaceOrderStatus result = converter.convertToEntityAttribute("CONFIRMED");
        assertEquals(MarketplaceOrderStatus.CONFIRMED, result);
    }

    @Test
    void convertToEntityAttribute_handlesNullOrBlank() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttribute_throwsForUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("INVALID_STATUS"));
    }

    @Test
    void convertToDatabaseColumn_writesCanonicalEnumName() {
        String result = converter.convertToDatabaseColumn(MarketplaceOrderStatus.PENDING_PAYMENT);
        assertEquals("PENDING_PAYMENT", result);
    }
}
