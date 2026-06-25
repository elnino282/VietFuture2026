package org.example.inventory.event;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class ProductWarehouseLotReceivedEvent extends DomainEvent {
    private Integer lotId;
    private Integer harvestId;
    private Integer seasonId;
    private Integer farmId;
    private Integer warehouseId;
    private BigDecimal quantity;
    private String unit;

    public ProductWarehouseLotReceivedEvent(String aggregateType, String aggregateId) {
        super(aggregateType, aggregateId);
    }

    @Override
    public String getEventType() {
        return "PRODUCT_WAREHOUSE_LOT_RECEIVED";
    }
}
