package org.example.farm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.farm.config.RabbitMQConfig;
import org.example.farm.dto.event.FarmUpdatedEvent;
import org.example.farm.entity.OutboxEvent;
import org.example.farm.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String routingKey = "farm.event." + event.getEventType().toLowerCase().replace("_", ".");
                
                // Deserialize payload to FarmUpdatedEvent to send it as native JSON object
                FarmUpdatedEvent eventPayload = objectMapper.readValue(event.getPayload(), FarmUpdatedEvent.class);
                
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, eventPayload);
                
                event.setProcessed(true);
                outboxEventRepository.save(event);
                
                log.info("Successfully published outbox event ID: {} with routingKey: {}", event.getId(), routingKey);
            } catch (Exception e) {
                log.error("Failed to publish outbox event ID: {}", event.getId(), e);
            }
        }
    }
}
