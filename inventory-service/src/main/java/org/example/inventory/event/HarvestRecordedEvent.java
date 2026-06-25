package org.example.inventory.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class HarvestRecordedEvent extends DomainEvent {

    private Integer harvestId;
    private Integer seasonId;
    private String seasonName;
    private Integer plotId;
    private Integer farmId;
    private String cropName;
    private String varietyName;
    private LocalDate harvestDate;
    private BigDecimal quantity;
    private String unit;
    private String grade;
    private String note;
    private Long actorUserId;

    public HarvestRecordedEvent(String aggregateType, String aggregateId) {
        super(aggregateType, aggregateId);
    }

    @Override
    public String getEventType() {
        return "HARVEST_RECORDED";
    }
}
