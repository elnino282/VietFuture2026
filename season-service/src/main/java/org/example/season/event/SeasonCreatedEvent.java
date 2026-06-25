package org.example.season.event;

import lombok.Getter;
import org.example.season.entity.Season;

@Getter
public class SeasonCreatedEvent extends DomainEvent {

    private final Integer seasonId;
    private final String seasonName;
    private final Integer plotId;
    private final Integer cropId;

    public SeasonCreatedEvent(Season season) {
        super("Season", season.getId() != null ? season.getId().toString() : "unknown");
        this.seasonId = season.getId();
        this.seasonName = season.getSeasonName();
        this.plotId = season.getPlotId();
        this.cropId = season.getCropId();
    }

    @Override
    public String getEventType() {
        return "SEASON_CREATED";
    }
}
