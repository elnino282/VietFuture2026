package org.example.inventory.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class HarvestRecordedEvent extends DomainEvent {
    private Integer harvestId;
    private Integer seasonId;
    private LocalDate harvestDate;
    private BigDecimal quantity;
    private BigDecimal unit;

    public HarvestRecordedEvent(String aggregateType, String aggregateId) {
        super(aggregateType, aggregateId);
    }

    @Override
    public String getEventType() {
        return "HARVEST_RECORDED";
    }
}
