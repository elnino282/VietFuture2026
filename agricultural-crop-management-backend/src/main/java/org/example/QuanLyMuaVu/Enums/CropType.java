package org.example.QuanLyMuaVu.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * High-level crop type group.
 */
public enum CropType {

    /**
     * Rice / Lúa.
     */
    RICE("RICE", "Rice / Lúa"),

    /**
     * Vegetable / Rau màu.
     */
    VEGETABLE("VEGETABLE", "Vegetable / Rau màu"),

    /**
     * Fruit tree / Cây ăn trái.
     */
    FRUIT("FRUIT", "Fruit tree / Cây ăn trái"),

    /**
     * Industrial crop / Cây công nghiệp.
     */
    INDUSTRIAL("INDUSTRIAL", "Industrial crop / Cây công nghiệp"),

    /**
     * Flower / Hoa, cây cảnh.
     */
    FLOWER("FLOWER", "Flower / Hoa, cây cảnh"),

    /**
     * Other / Khác.
     */
    OTHER("OTHER", "Other / Khác");

    private final String code;
    private final String displayName;

    CropType(String code, String displayName) {
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
     * Creates a CropType from its code (case-insensitive).
     *
     * @param code the persisted/JSON code
     * @return matching CropType or null if input is null
     * @throws IllegalArgumentException if the code is unknown
     */
    @JsonCreator
    public static CropType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CropType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CropType code: " + code);
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
