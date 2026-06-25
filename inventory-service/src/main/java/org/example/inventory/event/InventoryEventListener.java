package org.example.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventory.config.RabbitMQConfig;
import org.example.inventory.entity.ProductWarehouseLot;
import org.example.inventory.service.ProductWarehouseService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final ObjectMapper objectMapper;
    private final ProductWarehouseService productWarehouseService;

    @RabbitListener(queues = RabbitMQConfig.HARVEST_RECORDED_QUEUE)
    public void handleHarvestRecordedEvent(Object payload) {
        try {
            HarvestRecordedEvent event = objectMapper.convertValue(payload, HarvestRecordedEvent.class);
            log.info("Received HARVEST_RECORDED event: {}", event.getHarvestId());
            ProductWarehouseLot lot = productWarehouseService.receiveFromHarvestEvent(
                    event.getHarvestId(),
                    event.getSeasonId(),
                    event.getSeasonName(),
                    event.getPlotId(),
                    event.getFarmId(),
                    event.getCropName(),
                    event.getVarietyName(),
                    event.getHarvestDate(),
                    event.getQuantity(),
                    event.getUnit(),
                    event.getGrade(),
                    event.getNote(),
                    event.getActorUserId()
            );
            log.info("Successfully created product warehouse lot: {}", lot.getId());
        } catch (Exception e) {
            log.error("Error processing HARVEST_RECORDED event", e);
        }
    }
}
