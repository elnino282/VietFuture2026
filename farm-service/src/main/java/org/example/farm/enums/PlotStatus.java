package org.example.farm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlotStatus {
    AVAILABLE("AVAILABLE", "Available"),
    IN_USE("IN_USE", "In use"),
    IDLE("IDLE", "Idle"),
    FALLOW("FALLOW", "Fallow / Resting"),
    MAINTENANCE("MAINTENANCE", "Under maintenance");

    private final String code;
    private final String displayName;

    PlotStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static PlotStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PlotStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown PlotStatus code: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
