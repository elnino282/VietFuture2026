package org.example.marketplace.model;

public enum MarketplaceOrderStatus {
    PENDING_RESERVATION,
    PENDING,
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
