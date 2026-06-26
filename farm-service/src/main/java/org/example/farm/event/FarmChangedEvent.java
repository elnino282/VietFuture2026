package org.example.farm.event;

import java.math.BigDecimal;
import lombok.Getter;
import org.example.farm.entity.Farm;

@Getter
public class FarmChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer farmId;
    private final String farmName;
    private final Long userId;
    private final Integer provinceId;
    private final String provinceName;
    private final Integer wardId;
    private final String wardName;
    private final java.math.BigDecimal area;
    private final java.math.BigDecimal latitude;
    private final java.math.BigDecimal longitude;
    private final Boolean active;

    public FarmChangedEvent(Farm farm, Action action) {
        super("Farm",
              farm.getId() != null ? farm.getId().toString() : "unknown",
              "farm-service",
              "farm.event.farm." + action.name().toLowerCase());
        this.action = action;
        this.farmId = farm.getId();
        this.farmName = farm.getName();
        this.userId = farm.getUserId();
        this.provinceId = farm.getProvince() != null ? farm.getProvince().getId() : null;
        this.provinceName = farm.getProvince() != null ? farm.getProvince().getName() : null;
        this.wardId = farm.getWard() != null ? farm.getWard().getId() : null;
        this.wardName = farm.getWard() != null ? farm.getWard().getName() : null;
        this.area = farm.getArea();
        this.latitude = farm.getLatitude();
        this.longitude = farm.getLongitude();
        this.active = farm.getActive();
    }
}
