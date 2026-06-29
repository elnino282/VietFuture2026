package org.example.marketplace.event;

import java.time.LocalDateTime;

public record MarketplaceProductChangedEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        LocalDateTime occurredAt,
        Payload payload
) implements DomainEvent {

    public record Payload(
            Long productId,
            String productName,
            Integer farmId,
            String farmName,
            Long farmerId,
            String farmerName,
            String status,
            String updatedAt
    ) {}

    public static String getEventType() {
        return "MarketplaceProductChanged";
    }

    public static String getRoutingKey() {
        return "marketplace.product.changed";
    }
}
