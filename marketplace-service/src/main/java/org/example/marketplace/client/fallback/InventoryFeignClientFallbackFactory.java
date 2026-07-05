package org.example.marketplace.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.InventoryFeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InventoryFeignClientFallbackFactory implements FallbackFactory<InventoryFeignClient> {

    @Override
    public InventoryFeignClient create(Throwable cause) {
        return new InventoryFeignClient() {
            @Override
            public Map<String, Object> getLotDetail(Integer lotId, String token) {
                if (cause instanceof FeignException.NotFound) {
                    return null;
                }
                log.error("inventory-service lỗi khi lấy lotDetail lotId={}: {}", lotId, cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> reserveStock(String idempotencyKey, String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi reserveStock: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> releaseReservation(String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi releaseReservation: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> confirmStockOut(String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi confirmStockOut: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public List<Map<String, Object>> getAvailableStock(List<Integer> lotIds, String token) {
                log.error("inventory-service lỗi khi getAvailableStock: {}", cause.toString());
                return Collections.emptyList();
            }

            @Override
            public List<Map<String, Object>> getLotsBySeasonIds(List<Integer> seasonIds, String token) {
                log.error("inventory-service lỗi khi getLotsBySeasonIds: {}", cause.toString());
                return Collections.emptyList();
            }
        };
    }
}
