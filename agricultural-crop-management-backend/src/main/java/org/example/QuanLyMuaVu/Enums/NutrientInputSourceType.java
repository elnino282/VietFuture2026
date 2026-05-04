package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum NutrientInputSourceType {
    USER_ENTERED(true, "user_entered"),
    LAB_MEASURED(true, "lab_measured"),
    SYSTEM_ESTIMATED(false, "system_estimated"),
    EXTERNAL_REFERENCE(false, "external_reference"),
    DEFAULT_REFERENCE(false, "default_reference");

    private final boolean measured;
    private final String apiValue;

    NutrientInputSourceType(boolean measured, String apiValue) {
        this.measured = measured;
        this.apiValue = apiValue;
    }

    public boolean isMeasured() {
        return measured;
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }

    @JsonCreator
    public static NutrientInputSourceType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (NutrientInputSourceType candidate : values()) {
            if (candidate.apiValue.equals(normalized) || candidate.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported nutrient input source type: " + value);
    }
}
