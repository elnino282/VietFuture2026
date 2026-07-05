package org.example.marketplace.client.fallback;

import org.example.marketplace.client.InventoryFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InventoryFeignClientFallback implements InventoryFeignClient {

    @Override
    public Map<String, Object> getLotDetail(Integer lotId, String token) {
        log.error("Fallback triggered: Failed to get lot detail for lotId={}", lotId);
        return null;
    }

    @Override
    public Map<String, Object> reserveStock(String idempotencyKey, String token, Map<String, Object> requestBody) {
        log.error("Fallback triggered: Failed to reserve stock with idempotencyKey={}", idempotencyKey);
        return null;
    }

    @Override
    public Map<String, Object> releaseReservation(String token, Map<String, Object> requestBody) {
        log.error("Fallback triggered: Failed to release reservation");
        return null;
    }

    @Override
    public Map<String, Object> confirmStockOut(String token, Map<String, Object> requestBody) {
        log.error("Fallback triggered: Failed to confirm stock-out");
        return null;
    }

    @Override
    public List<Map<String, Object>> getAvailableStock(List<Integer> lotIds, String token) {
        log.error("Fallback triggered: Failed to get available stock for lotIds");
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getLotsBySeasonIds(List<Integer> seasonIds, String token) {
        log.error("Fallback triggered: Failed to get lots by season IDs");
        return Collections.emptyList();
    }
}
