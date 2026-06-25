package org.example.inventory.event;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StockAdjustedEvent extends DomainEvent {

    private Integer productWarehouseLotId;
    private String lotCode;
    private Integer farmId;
    private BigDecimal previousQuantity;
    private BigDecimal newQuantity;
    private BigDecimal quantityChange;
    private String unit;
    private String reason;
    private Long actorUserId;

    public StockAdjustedEvent(String aggregateType, String aggregateId) {
        super(aggregateType, aggregateId);
    }

    @Override
    public String getEventType() {
        return "STOCK_ADJUSTED";
    }
}
