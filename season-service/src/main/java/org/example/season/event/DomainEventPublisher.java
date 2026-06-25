package org.example.season.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.season.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RabbitTemplate rabbitTemplate;

    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }

        // Publish locally via Spring Event Publisher
        applicationEventPublisher.publishEvent(event);

        // Publish to RabbitMQ
        try {
            String routingKey = "season.event." + event.getEventType().toLowerCase().replace("_", ".");
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
            log.info("Successfully published event {} to RabbitMQ with routing key {}", event.getEventType(), routingKey);
        } catch (Exception e) {
            log.error("Failed to publish event {} to RabbitMQ", event.getEventType(), e);
        }
    }
}
