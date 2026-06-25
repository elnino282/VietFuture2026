package org.example.season.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    PENDING("PENDING", "Pending"),
    IN_PROGRESS("IN_PROGRESS", "In progress"),
    DONE("DONE", "Done"),
    OVERDUE("OVERDUE", "Overdue"),
    CANCELLED("CANCELLED", "Cancelled");

    private final String code;
    private final String displayName;

    TaskStatus(String code, String displayName) {
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
    public static TaskStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus code: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
