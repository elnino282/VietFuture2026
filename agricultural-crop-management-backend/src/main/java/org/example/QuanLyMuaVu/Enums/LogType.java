package org.example.QuanLyMuaVu.Enums;

import lombok.Getter;

/**
 * Whitelisted log types for field log entries.
 */
@Getter
public enum LogType {
    TRANSPLANT("Transplant seedlings"),
    FERTILIZE("Apply fertilizer"),
    PEST("Pest observation/treatment"),
    SPRAY("Spray pesticide/herbicide"),
    IRRIGATE("Irrigation activity"),
    WEED("Weeding activity"),
    HARVEST("Harvest activity"),
    WEATHER("Weather observation"),
    GROWTH("Growth stage observation"),
    OTHER("Other activity");

    private final String description;

    LogType(String description) {
        this.description = description;
    }

    /**
     * Check if a given string is a valid LogType.
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            LogType.valueOf(value.toUpperCase().trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
