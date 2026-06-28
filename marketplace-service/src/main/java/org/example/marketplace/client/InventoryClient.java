package org.example.marketplace.client;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryClient {
    
    LotDetailDto getLotDetail(Integer lotId);
    
    List<LotDetailDto> getLotsByIds(List<Integer> lotIds);

    List<LotDetailDto> getLotsBySeasonIds(List<Integer> seasonIds);
    
    ReservationResult reserveStock(String idempotencyKey, Long orderId, List<ReserveItem> items);
    
    ReservationResult releaseReservation(Long orderId, String reason);
    
    ReservationResult confirmStockOut(Long orderId, String reason);
    
    List<AvailableStock> getAvailableStock(List<Integer> lotIds);
    
    record LotDetailDto(Integer id, String lotCode, Integer farmId, Integer seasonId,
                       String productName, String productVariant, String unit,
                       BigDecimal initialQuantity, BigDecimal onHandQuantity, String status) {}
    
    record ReserveItem(Long orderItemId, Integer lotId, String lotCode, BigDecimal quantity, String unit) {}
    
    record ReservationResult(boolean success, String message, List<ReservedItem> items) {
        public record ReservedItem(Long itemId, Integer lotId, BigDecimal quantity, BigDecimal previousOnHand, BigDecimal newOnHand) {}
    }
    
    record AvailableStock(Integer lotId, String lotCode, BigDecimal onHand, BigDecimal reserved, BigDecimal available, String unit) {}
}
