package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an agricultural season.
 */
public enum SeasonStatus {

    /**
     * Season created but not started.
     */
    PLANNED("PLANNED", "Planned"),

    /**
     * Season is currently ongoing.
     */
    ACTIVE("ACTIVE", "Active"),

    /**
     * Season finished successfully.
     */
    COMPLETED("COMPLETED", "Completed"),

    /**
     * Season was cancelled.
     */
    CANCELLED("CANCELLED", "Cancelled"),

    /**
     * Season is closed and archived as read-only.
     */
    ARCHIVED("ARCHIVED", "Archived");

    private final String code;
    private final String displayName;

    SeasonStatus(String code, String displayName) {
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
     * Creates a SeasonStatus from its code (case-insensitive).
     *
     * @param code the persisted/JSON code
     * @return matching SeasonStatus or null if input is null
     * @throws IllegalArgumentException if the code is unknown
     */
    @JsonCreator
    public static SeasonStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SeasonStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SeasonStatus code: " + code);
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
