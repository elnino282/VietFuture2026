package org.example.farm.event;

import java.math.BigDecimal;
import lombok.Getter;
import org.example.farm.entity.Plot;

@Getter
public class PlotChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer plotId;
    private final Integer farmId;
    private final String plotName;
    private final BigDecimal area;
    private final String soilType;
    private final String boundaryGeoJson;
    private final String status;

    public PlotChangedEvent(Plot plot, Action action) {
        super("Plot",
              plot.getId() != null ? plot.getId().toString() : "unknown",
              "farm-service",
              "farm.event.plot." + action.name().toLowerCase());
        this.action = action;
        this.plotId = plot.getId();
        this.farmId = plot.getFarm() != null ? plot.getFarm().getId() : null;
        this.plotName = plot.getPlotName();
        this.area = plot.getArea();
        this.soilType = plot.getSoilType();
        this.boundaryGeoJson = plot.getBoundaryGeoJson();
        this.status = plot.getStatus();
    }
}
