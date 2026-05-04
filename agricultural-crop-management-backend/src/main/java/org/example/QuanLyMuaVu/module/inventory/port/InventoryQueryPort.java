package org.example.QuanLyMuaVu.module.inventory.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;

public interface InventoryQueryPort {

    List<InventoryLowStockView> findLowStockByOwnerId(Long ownerId, int limit, BigDecimal threshold);

    long countExpiringLotsByOwnerId(Long ownerId, LocalDate expiryThreshold);

    boolean existsProductWarehouseLotByHarvestId(Integer harvestId);

    Optional<HarvestStockContextView> findHarvestStockContext(
            Integer farmId,
            Integer warehouseId,
            String productName,
            String lotCode);

    List<SupplyLot> findAllSupplyLots();

    Optional<SupplyLot> findSupplyLotByIdWithDetails(Integer lotId);

    boolean existsSupplyLotById(Integer lotId);

    List<InventoryBalance> findAllInventoryBalancesWithDetails();

    List<InventoryBalance> findInventoryBalancesBySupplyLotId(Integer lotId);

    List<StockMovement> findStockMovementsBySupplyLotId(Integer lotId);
}
