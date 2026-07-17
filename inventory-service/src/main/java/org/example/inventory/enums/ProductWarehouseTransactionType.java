package org.example.inventory.enums;

public enum ProductWarehouseTransactionType {
    RECEIPT_FROM_HARVEST,
    ADJUSTMENT,
    STOCK_OUT,
    TRANSFER,
    RETURN,
    MARKETPLACE_ORDER_RESERVED,
    MARKETPLACE_ORDER_RELEASED,
    MARKETPLACE_ORDER_SOLD,
    // === Substandard Disposal ===
    STOCK_OUT_SUBSTANDARD,
    SOLD_LIVESTOCK_FEED,
    COMPOSTED,
    DISCARDED;

    public static ProductWarehouseTransactionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return ProductWarehouseTransactionType.valueOf(code.trim().toUpperCase());
    }
}
