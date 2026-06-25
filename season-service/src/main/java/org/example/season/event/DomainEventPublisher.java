package org.example.season.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.season.entity.OutboxEvent;
import org.example.season.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }

        // Save event to outbox
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId().toString())
                    .eventType(event.getEventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .processed(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Successfully saved event {} to outbox", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to save event {} to outbox", event.getEventType(), e);
        }
    }
}
