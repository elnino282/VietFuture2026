package org.example.season.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.example.season.entity.Harvest;

@Getter
public class HarvestRecordedEvent extends DomainEvent {

    private final Integer harvestId;
    private final Integer seasonId;
    private final LocalDate harvestDate;
    private final BigDecimal quantity;
    private final BigDecimal unit;

    public HarvestRecordedEvent(Harvest harvest) {
        super("Harvest", harvest.getId() != null ? harvest.getId().toString() : "unknown");
        this.harvestId = harvest.getId();
        this.seasonId = harvest.getSeason() != null ? harvest.getSeason().getId() : null;
        this.harvestDate = harvest.getHarvestDate();
        this.quantity = harvest.getQuantity();
        this.unit = harvest.getUnit();
    }

    @Override
    public String getEventType() {
        return "HARVEST_RECORDED";
    }
}
