package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Observer Pattern: Base Domain Event.
 * <p>
 * Base class for all domain events in the system. Uses Spring's
 * ApplicationEventPublisher for decoupled event handling.
 * <p>
 * Benefits:
 * - Decouples event producers from consumers
 * - Easy to add new listeners without modifying existing code
 * - Supports async processing for non-blocking operations
 * - Natural audit trail through event persistence
 */
public abstract class DomainEvent {

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

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Returns the event type name for logging/auditing.
     */
    public abstract String getEventType();
}
