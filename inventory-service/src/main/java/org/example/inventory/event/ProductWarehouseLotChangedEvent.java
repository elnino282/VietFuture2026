package org.example.inventory.event;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class ProductWarehouseLotChangedEvent extends DomainEvent {

    private final Integer lotId;
    private final String lotCode;
    private final Integer farmId;
    private final Integer warehouseId;
    private final BigDecimal quantityOnHand;
    private final String status;
    private final String action;

    public ProductWarehouseLotChangedEvent(String aggregateType,
                                           String aggregateId,
                                           String producer,
                                           Integer lotId,
                                           String lotCode,
                                           Integer farmId,
                                           Integer warehouseId,
                                           BigDecimal quantityOnHand,
                                           String status,
                                           String action) {
        super(aggregateType, aggregateId, producer, "inventory.event.lot." + action.toLowerCase());
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.farmId = farmId;
        this.warehouseId = warehouseId;
        this.quantityOnHand = quantityOnHand;
        this.status = status;
        this.action = action;
    }
}
