package org.example.QuanLyMuaVu.Enums;

public enum IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH;

    public static IncidentSeverity fromCode(String code) {
        if (code == null) {
            return null;
        }
        return IncidentSeverity.valueOf(code.toUpperCase());
    }
}

