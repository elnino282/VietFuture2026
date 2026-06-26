package org.example.cropcatalog.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cropcatalog.config.RabbitMQConfig;
import org.example.cropcatalog.entity.OutboxEvent;
import org.example.cropcatalog.repository.OutboxEventRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.outbox-publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

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
                String routingKey = event.getEventType();

                Message message = MessageBuilder
                        .withBody(event.getPayload().getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(event.getId())
                        .setHeader("eventType", event.getEventType())
                        .build();

                rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, routingKey, message);

                event.setProcessed(true);
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event ID: {} with routingKey: {}", event.getId(), routingKey);
            } catch (Exception e) {
                log.error("Failed to publish outbox event ID: {}", event.getId(), e);
            }
        }
    }
}
