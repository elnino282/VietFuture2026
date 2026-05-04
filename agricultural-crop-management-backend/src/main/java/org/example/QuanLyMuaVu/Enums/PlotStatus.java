package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an agricultural plot/field.
 */
public enum PlotStatus {

    /**
     * Plot is free and can be assigned to a new season.
     */
    AVAILABLE("AVAILABLE", "Available"),

    /**
     * Plot is currently in use for an active season.
     */
    IN_USE("IN_USE", "In use"),

    /**
     * Plot is idle and ready for use (same as AVAILABLE but per business
     * requirement).
     */
    IDLE("IDLE", "Idle"),

    /**
     * Plot is left fallow (resting between seasons).
     */
    FALLOW("FALLOW", "Fallow / Resting"),

    /**
     * Plot is under maintenance and not used for production.
     */
    MAINTENANCE("MAINTENANCE", "Under maintenance");

    private final String code;
    private final String displayName;

    PlotStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Returns the stable code used for persistence and JSON.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Creates a PlotStatus from its code (case-insensitive).
     *
     * @param code the persisted/JSON code
     * @return matching PlotStatus or null if input is null
     * @throws IllegalArgumentException if the code is unknown
     */
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

    /**
     * Serializes this enum as its code in JSON.
     *
     * @return the code used in JSON
     */
    @JsonValue
    public String toJson() {
        return this.code;
    }
}
