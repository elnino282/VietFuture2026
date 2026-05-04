package org.example.QuanLyMuaVu.Enums;

public enum NutrientInputSource {
    MINERAL_FERTILIZER(true),
    ORGANIC_FERTILIZER(true),
    BIOLOGICAL_FIXATION(false),
    IRRIGATION_WATER(false),
    ATMOSPHERIC_DEPOSITION(false),
    SEED_IMPORT(true),
    SOIL_LEGACY(false),
    CONTROL_SUPPLY(false);

    private final boolean humanControlled;

    NutrientInputSource(boolean humanControlled) {
        this.humanControlled = humanControlled;
    }

    public boolean isHumanControlled() {
        return humanControlled;
    }
}
