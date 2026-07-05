package org.example.marketplace.client.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.InventoryClient;
import org.example.marketplace.client.InventoryFeignClient;
import org.example.marketplace.client.InventoryClient.AvailableStock;
import org.example.marketplace.client.InventoryClient.LotDetailDto;
import org.example.marketplace.client.InventoryClient.ReservationResult;
import org.example.marketplace.client.InventoryClient.ReservationResult.ReservedItem;
import org.example.marketplace.client.InventoryClient.ReserveItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClientImpl implements InventoryClient {

    private final InventoryFeignClient inventoryFeignClient;

    private String getToken() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                return "Bearer " + jwt.getTokenValue();
            }
        } catch (Exception e) {
            log.warn("Failed to get security context for FeignClient: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public LotDetailDto getLotDetail(Integer lotId) {
        try {
            Map<String, Object> response = inventoryFeignClient.getLotDetail(lotId, getToken());
            if (response == null) {
                return null;
            }

            return mapToLotDetailDto(response);
        } catch (Exception e) {
            log.error("Failed to get lot detail: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<LotDetailDto> getLotsByIds(List<Integer> lotIds) {
        return lotIds.stream()
                .map(this::getLotDetail)
                .toList();
    }

    @Override
    public ReservationResult reserveStock(String idempotencyKey, Long orderId, List<ReserveItem> items) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "idempotencyKey", idempotencyKey,
                    "orderId", orderId,
                    "items", items
            );

            Map<String, Object> response = inventoryFeignClient.reserveStock(idempotencyKey, getToken(), requestBody);

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            return mapToReservationResult(response);
        } catch (Exception e) {
            log.error("Failed to reserve stock in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to reserve stock: " + e.getMessage(), List.of());
        }
    }

    @Override
    public ReservationResult releaseReservation(Long orderId, String reason) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "orderId", orderId,
                    "reason", reason != null ? reason : ""
            );

            Map<String, Object> response = inventoryFeignClient.releaseReservation(getToken(), requestBody);

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            return mapToReservationResult(response);
        } catch (Exception e) {
            log.error("Failed to release reservation in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to release reservation: " + e.getMessage(), List.of());
        }
    }

    @Override
    public ReservationResult confirmStockOut(Long orderId, String reason) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "orderId", orderId,
                    "reason", reason != null ? reason : ""
                );

            Map<String, Object> response = inventoryFeignClient.confirmStockOut(getToken(), requestBody);

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            return mapToReservationResult(response);
        } catch (Exception e) {
            log.error("Failed to confirm stock-out in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to confirm stock-out: " + e.getMessage(), List.of());
        }
    }

    @Override
    public List<AvailableStock> getAvailableStock(List<Integer> lotIds) {
        try {
            List<Map<String, Object>> response = inventoryFeignClient.getAvailableStock(lotIds, getToken());

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(item -> new AvailableStock(
                            (Integer) item.get("lotId"),
                            (String) item.get("lotCode"),
                            getBigDecimal(item.get("onHandQuantity")),
                            getBigDecimal(item.get("reservedQuantity")),
                            getBigDecimal(item.get("availableQuantity")),
                            (String) item.get("unit")
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get available stock from inventory service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<LotDetailDto> getLotsBySeasonIds(List<Integer> seasonIds) {
        if (seasonIds == null || seasonIds.isEmpty()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> response = inventoryFeignClient.getLotsBySeasonIds(seasonIds, getToken());

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(this::mapToLotDetailDto)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get lots by season IDs from inventory service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private LotDetailDto mapToLotDetailDto(Map<String, Object> response) {
        return new LotDetailDto(
                (Integer) response.get("id"),
                (String) response.get("lotCode"),
                (Integer) response.get("farmId"),
                (Integer) response.get("seasonId"),
                (String) response.get("productName"),
                (String) response.get("productVariant"),
                (String) response.get("unit"),
                getBigDecimal(response.get("initialQuantity")),
                getBigDecimal(response.get("onHandQuantity")),
                (String) response.get("status"),
                (String) response.get("farmName"),
                (String) response.get("seasonName"),
                (Integer) response.get("plotId"),
                (String) response.get("plotName"),
                (Integer) response.get("harvestId"),
                (String) response.get("warehouseName"),
                (String) response.get("locationLabel"),
                parseLocalDate(response.get("harvestedAt")),
                parseLocalDateTime(response.get("receivedAt")),
                (String) response.get("grade"),
                (String) response.get("qualityStatus")
        );
    }

    private ReservationResult mapToReservationResult(Map<String, Object> response) {
        String status = (String) response.get("status");
        boolean success = "RESERVED".equals(status) || "ALREADY_EXISTS".equals(status);
        String message = (String) response.get("message");
        
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) response.get("items");
        List<ReservedItem> reservedItems = itemsList == null ? List.of() : itemsList.stream()
                .map(item -> new ReservedItem(
                        getLong(item.get("itemId")),
                        (Integer) item.get("lotId"),
                        getBigDecimal(item.get("quantity")),
                        getBigDecimal(item.get("previousOnHand")),
                        getBigDecimal(item.get("newOnHand"))
                ))
                .toList();

        return new ReservationResult(success, message, reservedItems);
    }

    private BigDecimal getBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof Integer) return BigDecimal.valueOf((Integer) obj);
        if (obj instanceof Double) return BigDecimal.valueOf((Double) obj);
        if (obj instanceof String) return new BigDecimal((String) obj);
        return BigDecimal.ZERO;
    }

    private Long getLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof String) return Long.valueOf((String) obj);
        return null;
    }
    
    private LocalDate parseLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return LocalDate.parse((String) obj);
        return null; // Handle other cases if needed
    }
    
    private LocalDateTime parseLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return LocalDateTime.parse((String) obj);
        return null; // Handle other cases if needed
    }
}
