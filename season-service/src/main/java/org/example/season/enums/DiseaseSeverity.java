package org.example.season.enums;

public enum DiseaseSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static DiseaseSeverity fromCode(String code) {
        if (code == null) {
            return null;
        }
        return DiseaseSeverity.valueOf(code.trim().toUpperCase());
    }
}
