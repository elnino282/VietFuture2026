package org.example.season.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.example.season.entity.Harvest;

@Getter
@SuperBuilder
public class HarvestRecordedEvent extends DomainEvent {

    private final Integer harvestId;
    private final Integer seasonId;
    private final String seasonName;
    private final Integer plotId;
    private final Integer farmId;
    private final String cropName;
    private final String varietyName;
    private final LocalDate harvestDate;
    private final BigDecimal quantity;
    private final String unit;
    private final String grade;
    private final String note;
    private final Long actorUserId;

    public HarvestRecordedEvent(Harvest harvest, Long actorUserId) {
        super("Harvest", harvest.getId() != null ? harvest.getId().toString() : "unknown");
        this.harvestId = harvest.getId();
        this.seasonId = harvest.getSeason() != null ? harvest.getSeason().getId() : null;
        this.seasonName = harvest.getSeason() != null ? harvest.getSeason().getSeasonName() : null;
        this.plotId = harvest.getSeason() != null ? harvest.getSeason().getPlotId() : null;
        this.farmId = harvest.getSeason() != null ? harvest.getSeason().getFarmId() : null;
        this.cropName = harvest.getSeason() != null && harvest.getSeason().getCropName() != null ? harvest.getSeason().getCropName() : null;
        this.varietyName = harvest.getSeason() != null && harvest.getSeason().getVarietyName() != null ? harvest.getSeason().getVarietyName() : null;
        this.harvestDate = harvest.getHarvestDate();
        this.quantity = harvest.getQuantity();
        this.unit = harvest.getUnit() != null ? harvest.getUnit().toString() : "kg";
        this.grade = harvest.getGrade();
        this.note = harvest.getNote();
        this.actorUserId = actorUserId;
    }

    @Override
    public String getEventType() {
        return "HARVEST_RECORDED";
    }
}
