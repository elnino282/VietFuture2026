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
import org.example.marketplace.client.InventoryClient.AvailableStock;
import org.example.marketplace.client.InventoryClient.LotDetailDto;
import org.example.marketplace.client.InventoryClient.ReservationResult;
import org.example.marketplace.client.InventoryClient.ReservationResult.ReservedItem;
import org.example.marketplace.client.InventoryClient.ReserveItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClientImpl implements InventoryClient {

    @Value("${external-services.inventory-service-url:http://inventory-service:8086}")
    private String inventoryServiceUrl;

    private final WebClient.Builder webClientBuilder;

    private WebClient getWebClient() {
        return webClientBuilder.baseUrl(inventoryServiceUrl).build();
    }

    private <T extends WebClient.RequestHeadersSpec<?>> T authorize(T requestSpec) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                requestSpec.header("Authorization", "Bearer " + jwt.getTokenValue());
            }
        } catch (Exception e) {
            log.warn("Failed to propagate security context to WebClient: {}", e.getMessage());
        }
        return requestSpec;
    }

    @Override
    public LotDetailDto getLotDetail(Integer lotId) {
        try {
            ProductWarehouseLotResponse response = authorize(getWebClient().get()
                    .uri("/api/v1/product-warehouses/lots/{id}", lotId))
                    .retrieve()
                    .bodyToMono(ProductWarehouseLotResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return null;
            }

            return new LotDetailDto(
                    response.id(),
                    response.lotCode(),
                    response.farmId(),
                    response.seasonId(),
                    response.productName(),
                    response.productVariant(),
                    response.unit(),
                    response.initialQuantity(),
                    response.onHandQuantity(),
                    response.status(),
                    response.farmName(),
                    response.seasonName(),
                    response.plotId(),
                    response.plotName(),
                    response.harvestId(),
                    response.warehouseName(),
                    response.locationLabel(),
                    response.harvestedAt(),
                    response.receivedAt(),
                    response.grade(),
                    response.qualityStatus()
            );
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
            LocalReservationResponse response = authorize(getWebClient().post()
                    .uri("/api/v1/inventory/reservations/reserve"))
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ReserveStockRequestBody(idempotencyKey, orderId, items))
                    .retrieve()
                    .bodyToMono(LocalReservationResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            boolean success = "RESERVED".equals(response.status()) || "ALREADY_EXISTS".equals(response.status());
            List<ReservedItem> reservedItems = response.items() == null ? List.of() : response.items().stream()
                    .map(item -> new ReservedItem(
                            item.itemId(),
                            item.lotId(),
                            item.quantity(),
                            item.previousOnHand() != null ? item.previousOnHand() : BigDecimal.ZERO,
                            item.newOnHand() != null ? item.newOnHand() : BigDecimal.ZERO
                    ))
                    .toList();

            return new ReservationResult(success, response.message(), reservedItems);
        } catch (Exception e) {
            log.error("Failed to reserve stock in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to reserve stock: " + e.getMessage(), List.of());
        }
    }

    @Override
    public ReservationResult releaseReservation(Long orderId, String reason) {
        try {
            LocalReservationResponse response = authorize(getWebClient().post()
                    .uri("/api/v1/inventory/reservations/release"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "orderId", orderId,
                            "reason", reason != null ? reason : ""
                    ))
                    .retrieve()
                    .bodyToMono(LocalReservationResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            boolean success = "RESERVED".equals(response.status());
            List<ReservedItem> releasedItems = response.items() == null ? List.of() : response.items().stream()
                    .map(item -> new ReservedItem(
                            item.itemId(),
                            item.lotId(),
                            item.quantity(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    ))
                    .toList();

            return new ReservationResult(success, response.message(), releasedItems);
        } catch (Exception e) {
            log.error("Failed to release reservation in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to release reservation: " + e.getMessage(), List.of());
        }
    }

    @Override
    public ReservationResult confirmStockOut(Long orderId, String reason) {
        try {
            LocalReservationResponse response = authorize(getWebClient().post()
                    .uri("/api/v1/inventory/reservations/confirm"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "orderId", orderId,
                            "reason", reason != null ? reason : ""
                    ))
                    .retrieve()
                    .bodyToMono(LocalReservationResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                return new ReservationResult(false, "No response from inventory service", List.of());
            }

            boolean success = "RESERVED".equals(response.status());
            List<ReservedItem> confirmedItems = response.items() == null ? List.of() : response.items().stream()
                    .map(item -> new ReservedItem(
                            item.itemId(),
                            item.lotId(),
                            item.quantity(),
                            item.previousOnHand() != null ? item.previousOnHand() : BigDecimal.ZERO,
                            item.newOnHand() != null ? item.newOnHand() : BigDecimal.ZERO
                    ))
                    .toList();

            return new ReservationResult(success, response.message(), confirmedItems);
        } catch (Exception e) {
            log.error("Failed to confirm stock-out in inventory service: {}", e.getMessage(), e);
            return new ReservationResult(false, "Failed to confirm stock-out: " + e.getMessage(), List.of());
        }
    }

    @Override
    public List<AvailableStock> getAvailableStock(List<Integer> lotIds) {
        try {
            String uri = "/api/v1/inventory/reservations/available?lotIds=" +
                    lotIds.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "&lotIds=" + b)
                            .orElse("");

            List<LocalAvailableStockResponse> response = authorize(getWebClient().get()
                    .uri(uri))
                    .retrieve()
                    .bodyToFlux(LocalAvailableStockResponse.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(item -> new AvailableStock(
                            item.lotId(),
                            item.lotCode(),
                            item.onHandQuantity(),
                            item.reservedQuantity() != null ? item.reservedQuantity() : BigDecimal.ZERO,
                            item.availableQuantity() != null ? item.availableQuantity() : BigDecimal.ZERO,
                            item.unit()
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
            String uri = "/api/v1/public/lookup/inventory/lots/by-seasons?seasonIds=" +
                    seasonIds.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "&seasonIds=" + b)
                            .orElse("");

            List<ProductWarehouseLotResponse> response = authorize(getWebClient().get()
                    .uri(uri))
                    .retrieve()
                    .bodyToFlux(ProductWarehouseLotResponse.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(item -> new LotDetailDto(
                            item.id(),
                            item.lotCode(),
                            item.farmId(),
                            item.seasonId(),
                            item.productName(),
                            item.productVariant(),
                            item.unit(),
                            item.initialQuantity(),
                            item.onHandQuantity(),
                            item.status(),
                            item.farmName(),
                            item.seasonName(),
                            item.plotId(),
                            item.plotName(),
                            item.harvestId(),
                            item.warehouseName(),
                            item.locationLabel(),
                            item.harvestedAt(),
                            item.receivedAt(),
                            item.grade(),
                            item.qualityStatus()
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get lots by season IDs from inventory service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private record ProductWarehouseLotResponse(
            Integer id,
            String lotCode,
            Integer productId,
            String productName,
            String productVariant,
            Integer seasonId,
            String seasonName,
            Integer farmId,
            String farmName,
            Integer plotId,
            String plotName,
            Integer harvestId,
            String warehouseName,
            String locationLabel,
            LocalDate harvestedAt,
            LocalDateTime receivedAt,
            String unit,
            BigDecimal initialQuantity,
            BigDecimal onHandQuantity,
            String grade,
            String qualityStatus,
            String status
    ) {}

    private record ReserveStockRequestBody(
            String idempotencyKey,
            Long orderId,
            List<ReserveItem> items
    ) {}

    private record LocalReservationResponse(
            Long reservationId,
            String idempotencyKey,
            Long orderId,
            List<LocalReservedItemResponse> items,
            String status,
            String message
    ) {
        public record LocalReservedItemResponse(
                Long itemId,
                Integer lotId,
                String lotCode,
                BigDecimal quantity,
                String unit,
                BigDecimal previousOnHand,
                BigDecimal newOnHand
        ) {}
    }

    private record LocalAvailableStockResponse(
            Integer lotId,
            String lotCode,
            BigDecimal onHandQuantity,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity,
            String unit
    ) {}
}
