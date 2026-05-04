package org.example.QuanLyMuaVu.module.shared.pattern.Chain;


/**
 * Chain of Responsibility Pattern: Validation Context.
 * <p>
 * Encapsulates all data needed for Season creation validation.
 * Using a context object instead of the raw DTO allows validators
 * to access pre-fetched entities without repeated DB lookups.
 */
public class SeasonValidationContext {

    private Integer plotId;
    private Integer cropId;
    private Integer varietyId;
    private java.time.LocalDate startDate;
    private java.time.LocalDate plannedHarvestDate;
    private java.time.LocalDate endDate;
    private String seasonName;
    private Integer expectedYieldKg;
    private Integer initialPlantCount;
    private String notes;

    // Pre-fetched entities (populated by validators or service)
    private org.example.QuanLyMuaVu.module.farm.entity.Plot plot;
    private org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop;
    private org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety;
    private org.example.QuanLyMuaVu.module.identity.entity.User currentUser;

    // Builder pattern for construction
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Integer getPlotId() {
        return plotId;
    }

    public Integer getCropId() {
        return cropId;
    }

    public Integer getVarietyId() {
        return varietyId;
    }

    public java.time.LocalDate getStartDate() {
        return startDate;
    }

    public java.time.LocalDate getPlannedHarvestDate() {
        return plannedHarvestDate;
    }

    public java.time.LocalDate getEndDate() {
        return endDate;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public Integer getExpectedYieldKg() {
        return expectedYieldKg;
    }

    public Integer getInitialPlantCount() {
        return initialPlantCount;
    }

    public String getNotes() {
        return notes;
    }

    public org.example.QuanLyMuaVu.module.farm.entity.Plot getPlot() {
        return plot;
    }

    public org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop getCrop() {
        return crop;
    }

    public org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety getVariety() {
        return variety;
    }

    public org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser() {
        return currentUser;
    }

    // Setters for validators to populate entities
    public void setPlot(org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        this.plot = plot;
    }

    public void setCrop(org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop) {
        this.crop = crop;
    }

    public void setVariety(org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety) {
        this.variety = variety;
    }

    public void setCurrentUser(org.example.QuanLyMuaVu.module.identity.entity.User currentUser) {
        this.currentUser = currentUser;
    }

    public static class Builder {
        private final SeasonValidationContext ctx = new SeasonValidationContext();

        public Builder plotId(Integer plotId) {
            ctx.plotId = plotId;
            return this;
        }

        public Builder cropId(Integer cropId) {
            ctx.cropId = cropId;
            return this;
        }

        public Builder varietyId(Integer varietyId) {
            ctx.varietyId = varietyId;
            return this;
        }

        public Builder startDate(java.time.LocalDate startDate) {
            ctx.startDate = startDate;
            return this;
        }

        public Builder plannedHarvestDate(java.time.LocalDate plannedHarvestDate) {
            ctx.plannedHarvestDate = plannedHarvestDate;
            return this;
        }

        public Builder endDate(java.time.LocalDate endDate) {
            ctx.endDate = endDate;
            return this;
        }

        public Builder seasonName(String seasonName) {
            ctx.seasonName = seasonName;
            return this;
        }

        public Builder expectedYieldKg(Integer expectedYieldKg) {
            ctx.expectedYieldKg = expectedYieldKg;
            return this;
        }

        public Builder initialPlantCount(Integer initialPlantCount) {
            ctx.initialPlantCount = initialPlantCount;
            return this;
        }

        public Builder notes(String notes) {
            ctx.notes = notes;
            return this;
        }

        public Builder currentUser(org.example.QuanLyMuaVu.module.identity.entity.User user) {
            ctx.currentUser = user;
            return this;
        }

        public SeasonValidationContext build() {
            return ctx;
        }
    }
}

