package org.example.QuanLyMuaVu.module.marketplace.model;

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
    REJECTED
}
