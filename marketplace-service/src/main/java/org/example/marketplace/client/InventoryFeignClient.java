package org.example.marketplace.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "inventory-service", url = "${external-services.inventory-service-url:http://inventory-service:8086}", fallbackFactory = org.example.marketplace.client.fallback.InventoryFeignClientFallbackFactory.class)
public interface InventoryFeignClient {

    @GetMapping("/api/v1/product-warehouses/lots/{id}")
    Map<String, Object> getLotDetail(@PathVariable("id") Integer lotId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/inventory/reservations/reserve")
    Map<String, Object> reserveStock(@RequestHeader("X-Idempotency-Key") String idempotencyKey, @RequestHeader("Authorization") String token, @RequestBody Map<String, Object> requestBody);

    @PostMapping("/api/v1/inventory/reservations/release")
    Map<String, Object> releaseReservation(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> requestBody);

    @PostMapping("/api/v1/inventory/reservations/confirm")
    Map<String, Object> confirmStockOut(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> requestBody);

    @GetMapping("/api/v1/inventory/reservations/available")
    List<Map<String, Object>> getAvailableStock(@RequestParam("lotIds") List<Integer> lotIds, @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/public/lookup/inventory/lots/by-seasons")
    List<Map<String, Object>> getLotsBySeasonIds(@RequestParam("seasonIds") List<Integer> seasonIds, @RequestHeader(value = "Authorization", required = false) String token);
}
