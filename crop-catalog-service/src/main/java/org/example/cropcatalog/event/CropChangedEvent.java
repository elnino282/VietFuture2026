package org.example.cropcatalog.event;

import java.math.BigDecimal;
import lombok.Getter;
import org.example.cropcatalog.entity.Crop;

@Getter
public class CropChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer cropId;
    private final String cropName;
    private final String description;
    private final BigDecimal nContentKgPerKgYield;

    public CropChangedEvent(Crop crop, Action action, BigDecimal nContentKgPerKgYield) {
        super("Crop",
              crop.getId() != null ? crop.getId().toString() : "unknown",
              "crop-catalog-service",
              "crop.event.crop." + action.name().toLowerCase());
        this.action = action;
        this.cropId = crop.getId();
        this.cropName = crop.getCropName();
        this.description = crop.getDescription();
        this.nContentKgPerKgYield = nContentKgPerKgYield;
    }
}
