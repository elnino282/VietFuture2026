package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        
        // 1. Publish locally to Spring event system
        applicationEventPublisher.publishEvent(event);

        // 2. Save event to outbox for microservice synchronization
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .processed(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Successfully saved event {} to outbox in monolith", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to save event {} to outbox in monolith", event.getEventType(), e);
        }
    }
}
