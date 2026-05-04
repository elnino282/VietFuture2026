package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a task or activity within a season.
 */
public enum TaskStatus {

    /**
     * Task not started yet.
     */
    PENDING("PENDING", "Pending"),

    /**
     * Task is currently being executed.
     */
    IN_PROGRESS("IN_PROGRESS", "In progress"),

    /**
     * Task completed successfully.
     */
    DONE("DONE", "Done"),

    /**
     * Task not finished but past its due date.
     */
    OVERDUE("OVERDUE", "Overdue"),

    /**
     * Task was cancelled.
     */
    CANCELLED("CANCELLED", "Cancelled");

    private final String code;
    private final String displayName;

    TaskStatus(String code, String displayName) {
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
     * Creates a TaskStatus from its code (case-insensitive).
     *
     * @param code the persisted/JSON code
     * @return matching TaskStatus or null if input is null
     * @throws IllegalArgumentException if the code is unknown
     */
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
