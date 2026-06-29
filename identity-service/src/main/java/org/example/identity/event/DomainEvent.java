package org.example.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final String aggregateId;
    private final String serviceId;
    private final String eventType;

    protected DomainEvent(String aggregateType, String aggregateId, String serviceId, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.serviceId = serviceId;
        this.eventType = eventType;
    }
}
