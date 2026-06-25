package org.example.season.enums;

public enum DiseaseStatus {
    OPEN,
    UNDER_TREATMENT,
    MONITORING,
    RESOLVED,
    CLOSED;

    public static DiseaseStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        return DiseaseStatus.valueOf(code.trim().toUpperCase());
    }
}
