package org.example.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventory.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.HARVEST_RECORDED_QUEUE)
    public void handleHarvestRecordedEvent(Object payload) {
        try {
            HarvestRecordedEvent event = objectMapper.convertValue(payload, HarvestRecordedEvent.class);
            log.info("Received HARVEST_RECORDED event: {}", event.getHarvestId());
            // TODO: Implement actual inventory processing
        } catch (Exception e) {
            log.error("Error processing HARVEST_RECORDED event", e);
        }
    }
}
