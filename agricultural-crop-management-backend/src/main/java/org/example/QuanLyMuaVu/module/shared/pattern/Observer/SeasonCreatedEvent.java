package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;

/**
 * Observer Pattern: org.example.QuanLyMuaVu.module.season.entity.Season Created Event.
 * <p>
 * Published when a new season is successfully created.
 * Listeners can use this to:
 * - Auto-generate default tasks from templates
 * - Send notifications to farm managers
 * - Update dashboards/statistics
 */
@Getter
public class SeasonCreatedEvent extends DomainEvent {

    private final Integer seasonId;
    private final String seasonName;
    private final Integer plotId;
    private final Integer farmId;
    private final Integer cropId;
    private final Long ownerUserId;

    public SeasonCreatedEvent(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        super("org.example.QuanLyMuaVu.module.season.entity.Season", season.getId() != null ? season.getId().toString() : "unknown");
        this.seasonId = season.getId();
        this.seasonName = season.getSeasonName();
        this.plotId = season.getPlot() != null ? season.getPlot().getId() : null;
        this.farmId = season.getPlot() != null && season.getPlot().getFarm() != null
                ? season.getPlot().getFarm().getId()
                : null;
        this.cropId = season.getCrop() != null ? season.getCrop().getId() : null;
        this.ownerUserId = season.getPlot() != null
                && season.getPlot().getFarm() != null
                && season.getPlot().getFarm().getUser() != null
                        ? season.getPlot().getFarm().getUser().getId()
                        : null;
    }

    @Override
    public String getEventType() {
        return "SEASON_CREATED";
    }
}
