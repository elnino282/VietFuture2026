package org.example.QuanLyMuaVu.Enums;

public enum TreatmentEffectiveness {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT;

    public static TreatmentEffectiveness fromCode(String code) {
        if (code == null) {
            return null;
        }
        return TreatmentEffectiveness.valueOf(code.trim().toUpperCase());
    }
}
