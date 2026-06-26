package org.example.season.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.example.season.entity.Season;

@Getter
public class SeasonChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        STATUS_CHANGED,
        COMPLETED
    }

    private final Action action;
    private final Integer seasonId;
    private final String seasonName;
    private final Integer plotId;
    private final Integer farmId;
    private final Integer cropId;
    private final Integer varietyId;
    private final LocalDate startDate;
    private final LocalDate plannedHarvestDate;
    private final LocalDate endDate;
    private final String status;
    private final Integer initialPlantCount;
    private final Integer currentPlantCount;
    private final BigDecimal expectedYieldKg;
    private final BigDecimal actualYieldKg;
    private final BigDecimal budgetAmount;
    private final String notes;

    private static String mapActionToRoutingKey(Action action) {
        switch (action) {
            case CREATED:
                return "created";
            case UPDATED:
                return "updated";
            case STATUS_CHANGED:
                return "status.changed";
            case COMPLETED:
                return "completed";
            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    public SeasonChangedEvent(Season season, Integer farmId, Action action) {
        super("Season",
                season.getId() != null ? season.getId().toString() : "unknown",
                "season-service",
                "season.event.season." + mapActionToRoutingKey(action));
        this.action = action;
        this.seasonId = season.getId();
        this.seasonName = season.getSeasonName();
        this.plotId = season.getPlotId();
        this.farmId = farmId;
        this.cropId = season.getCropId();
        this.varietyId = season.getVarietyId();
        this.startDate = season.getStartDate();
        this.plannedHarvestDate = season.getPlannedHarvestDate();
        this.endDate = season.getEndDate();
        this.status = season.getStatus() != null ? season.getStatus().name() : null;
        this.initialPlantCount = season.getInitialPlantCount();
        this.currentPlantCount = season.getCurrentPlantCount();
        this.expectedYieldKg = season.getExpectedYieldKg();
        this.actualYieldKg = season.getActualYieldKg();
        this.budgetAmount = season.getBudgetAmount();
        this.notes = season.getNotes();
    }
}
