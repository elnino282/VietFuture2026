package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.shared.config.RabbitMQConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to publish in monolith", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String routingKey = event.getEventType();
                MessageProperties properties = new MessageProperties();
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);

                rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, routingKey, message);
                
                event.setProcessed(true);
                outboxEventRepository.save(event);
                log.info("Successfully published monolith outbox event ID: {} with routingKey: {}", event.getId(), routingKey);
            } catch (Exception e) {
                log.error("Failed to publish monolith outbox event ID: {}", event.getId(), e);
            }
        }
    }
}
