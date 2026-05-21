package org.example.QuanLyMuaVu.module.marketplace.model;

import java.util.Locale;

/**
 * Full order lifecycle for marketplace orders.
 * <p>
 * Happy-path (bank transfer):
 * PENDING_PAYMENT → PAYMENT_SUBMITTED → PAYMENT_VERIFIED → CONFIRMED
 * → PREPARING → SHIPPED → DELIVERED → COMPLETED
 * <p>
 * Happy-path (COD):
 * Same flow — COD orders auto-advance through PAYMENT_SUBMITTED and
 * PAYMENT_VERIFIED immediately on creation.
 * <p>
 * Terminal states: COMPLETED, CANCELLED, REJECTED
 */
public enum MarketplaceOrderStatus {
    PENDING_PAYMENT,
    PAYMENT_SUBMITTED,
    PAYMENT_VERIFIED,
    CONFIRMED,
    PREPARING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    /**
     * Backward-compatible parser for status values persisted by legacy schema/data.
     */
    public static MarketplaceOrderStatus fromStorageValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING" -> PENDING_PAYMENT;
            case "DELIVERING" -> SHIPPED;
            default -> MarketplaceOrderStatus.valueOf(normalized);
        };
    }
}
