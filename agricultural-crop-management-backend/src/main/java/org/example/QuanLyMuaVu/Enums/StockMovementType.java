package org.example.QuanLyMuaVu.Enums;

public enum StockMovementType {
    IN,
    OUT,
    ADJUST;

    public static StockMovementType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return StockMovementType.valueOf(code.toUpperCase());
    }
}

