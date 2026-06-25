package org.example.season.enums;

public enum ProductWarehouseLotStatus {
    IN_STOCK,
    HOLD,
    DEPLETED,
    ARCHIVED;

    public static ProductWarehouseLotStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return ProductWarehouseLotStatus.valueOf(code.trim().toUpperCase());
    }
}
