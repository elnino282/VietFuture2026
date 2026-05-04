package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class HarvestChangedEvent extends DomainEvent {

    public enum Action {
        RECORDED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer harvestId;
    private final Integer seasonId;
    private final Long ownerUserId;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;

    public HarvestChangedEvent(org.example.QuanLyMuaVu.module.season.entity.Harvest harvest, Action action) {
        super("org.example.QuanLyMuaVu.module.season.entity.Harvest", harvest != null && harvest.getId() != null ? harvest.getId().toString() : "unknown");
        this.action = action;
        this.harvestId = harvest != null ? harvest.getId() : null;
        this.seasonId = harvest != null && harvest.getSeason() != null ? harvest.getSeason().getId() : null;
        this.ownerUserId = harvest != null
                && harvest.getSeason() != null
                && harvest.getSeason().getPlot() != null
                && harvest.getSeason().getPlot().getFarm() != null
                && harvest.getSeason().getPlot().getFarm().getUser() != null
                        ? harvest.getSeason().getPlot().getFarm().getUser().getId()
                        : null;
        this.quantity = harvest != null ? harvest.getQuantity() : null;
        this.unitPrice = harvest != null ? harvest.getUnit() : null;
    }

    @Override
    public String getEventType() {
        return "HARVEST_" + action.name();
    }
}
