package org.example.inventory.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class DomainEvent {
    private String aggregateType;
    private String aggregateId;

    public DomainEvent(String aggregateType, String aggregateId) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
    }

    public abstract String getEventType();
}
