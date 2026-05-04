package org.example.QuanLyMuaVu.Enums;

public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED;

    public static IncidentStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        return IncidentStatus.valueOf(code.toUpperCase());
    }
}

