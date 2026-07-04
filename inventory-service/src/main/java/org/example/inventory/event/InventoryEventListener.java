package org.example.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventory.config.RabbitMQConfig;
import org.example.inventory.entity.ProcessedEvent;
import org.example.inventory.entity.ProductWarehouseLot;
import org.example.inventory.repository.ProcessedEventRepository;
import org.example.inventory.service.ProductWarehouseService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final ObjectMapper objectMapper;
    private final ProductWarehouseService productWarehouseService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.HARVEST_RECORDED_QUEUE)
    public void handleHarvestRecordedEvent(Message message) {
        String eventId = message.getMessageProperties().getMessageId();
        if (eventId == null) {
            log.warn("Received event without messageId, skipping");
            return;
        }

        // Check idempotency
        Optional<ProcessedEvent> existing = processedEventRepository.findById(eventId);
        if (existing.isPresent()) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        try {
            HarvestRecordedEvent event;
            try {
                event = objectMapper.readValue(message.getBody(), HarvestRecordedEvent.class);
            } catch (Exception e) {
                log.error("Failed to deserialize event {}: {}", eventId, e.getMessage(), e);
                throw new RuntimeException("Failed to deserialize event", e);
            }
            log.info("Received event: {} of type {}", eventId, event.getEventType());
            ProductWarehouseLot lot = productWarehouseService.receiveFromHarvestEvent(event);
            log.info("Successfully created product warehouse lot: {}", lot.getId());

            // Mark event as processed
            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(eventId)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }
}
