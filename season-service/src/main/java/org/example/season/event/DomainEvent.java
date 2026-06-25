package org.example.season.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent implements Serializable {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final String aggregateId;

    protected DomainEvent(String aggregateType, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
    }

    public abstract String getEventType();
}
