package org.example.cropcatalog.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent implements Serializable {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime occurredAt;
    private final String aggregateType;
    private final String aggregateId;
    private final String producer;

    protected DomainEvent(String aggregateType, String aggregateId, String producer, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = LocalDateTime.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.producer = producer;
    }
}
