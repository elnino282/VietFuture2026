package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a system user (FARMER/BUYER).
 */
public enum UserStatus {

    ACTIVE("ACTIVE", "Active"),
    INACTIVE("INACTIVE", "Inactive"),
    LOCKED("LOCKED", "Locked");

    private final String code;
    private final String displayName;

    UserStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
        ;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static UserStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.code;
    }
}
