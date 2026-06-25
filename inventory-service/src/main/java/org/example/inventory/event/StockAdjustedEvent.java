package org.example.inventory.event;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class StockAdjustedEvent extends DomainEvent {
    private Integer productWarehouseLotId;
    private BigDecimal quantityChange;
    private String reason;

    public StockAdjustedEvent(String aggregateType, String aggregateId) {
        super(aggregateType, aggregateId);
    }

    @Override
    public String getEventType() {
        return "STOCK_ADJUSTED";
    }
}
