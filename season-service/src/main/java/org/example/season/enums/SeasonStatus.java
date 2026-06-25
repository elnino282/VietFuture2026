package org.example.season.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SeasonStatus {
    PLANNED("PLANNED", "Planned"),
    ACTIVE("ACTIVE", "Active"),
    COMPLETED("COMPLETED", "Completed"),
    CANCELLED("CANCELLED", "Cancelled"),
    ARCHIVED("ARCHIVED", "Archived");

    private final String code;
    private final String displayName;

    SeasonStatus(String code, String displayName) {
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

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
