package org.example.QuanLyMuaVu.module.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.port.HarvestStockContextView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryLowStockView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class InventoryQueryService implements InventoryQueryPort {

    FarmQueryPort farmQueryPort;
    WarehouseRepository warehouseRepository;
    StockMovementRepository stockMovementRepository;
    SupplyLotRepository supplyLotRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    ProductWarehouseLotRepository productWarehouseLotRepository;

    @Override
    public List<InventoryLowStockView> findLowStockByOwnerId(Long ownerId, int limit, BigDecimal threshold) {
        if (ownerId == null || limit <= 0) {
            return List.of();
        }

        BigDecimal effectiveThreshold = threshold != null ? threshold : BigDecimal.ZERO;
        List<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms = farmQueryPort.findFarmsByOwnerId(ownerId);
        if (farms.isEmpty()) {
            return List.of();
        }

        List<InventoryLowStockView> items = new ArrayList<>();
        for (org.example.QuanLyMuaVu.module.farm.entity.Farm farm : farms) {
            List<Warehouse> warehouses = warehouseRepository.findAllByFarm(farm);
            for (Warehouse warehouse : warehouses) {
                List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, null);
                for (Integer lotId : lotIds) {
                    SupplyLot lot = supplyLotRepository.findById(lotId).orElse(null);
                    if (lot == null) {
                        continue;
                    }

                    BigDecimal onHand = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, null);
                    if (onHand != null && onHand.compareTo(effectiveThreshold) <= 0) {
                        items.add(new InventoryLowStockView(
                                lot.getId(),
                                lot.getBatchCode(),
                                lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : "Unknown",
                                warehouse.getName(),
                                "",
                                onHand,
                                lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : "unit"));
                    }

                    if (items.size() >= limit) {
                        return items;
                    }
                }
            }
        }
        return items;
    }

    @Override
    public long countExpiringLotsByOwnerId(Long ownerId, LocalDate expiryThreshold) {
        if (ownerId == null || expiryThreshold == null) {
            return 0L;
        }

        List<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms = farmQueryPort.findFarmsByOwnerId(ownerId);
        if (farms.isEmpty()) {
            return 0L;
        }

        long expiringLots = 0L;
        for (org.example.QuanLyMuaVu.module.farm.entity.Farm farm : farms) {
            List<Warehouse> warehouses = warehouseRepository.findAllByFarm(farm);
            for (Warehouse warehouse : warehouses) {
                List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, null);
                for (Integer lotId : lotIds) {
                    SupplyLot lot = supplyLotRepository.findById(lotId).orElse(null);
                    if (lot != null && lot.getExpiryDate() != null
                            && !lot.getExpiryDate().isAfter(expiryThreshold)) {
                        expiringLots++;
                    }
                }
            }
        }
        return expiringLots;
    }

    @Override
    public boolean existsProductWarehouseLotByHarvestId(Integer harvestId) {
        if (harvestId == null) {
            return false;
        }
        return productWarehouseLotRepository.existsByHarvest_Id(harvestId);
    }

    @Override
    public Optional<HarvestStockContextView> findHarvestStockContext(
            Integer farmId,
            Integer warehouseId,
            String productName,
            String lotCode) {
        if (farmId == null
                || warehouseId == null
                || productName == null
                || productName.isBlank()
                || lotCode == null
                || lotCode.isBlank()) {
            return Optional.empty();
        }

        List<org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot> matchingLots =
                productWarehouseLotRepository.findByFarmWarehouseProductAndLotCode(
                        farmId,
                        warehouseId,
                        productName.trim(),
                        lotCode.trim());

        if (matchingLots.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal onHand = matchingLots.stream()
                .map(lot -> lot.getOnHandQuantity() != null ? lot.getOnHandQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot firstLot = matchingLots.get(0);
        return Optional.of(new HarvestStockContextView(
                matchingLots.size(),
                onHand,
                firstLot.getUnit(),
                firstLot.getWarehouse() != null ? firstLot.getWarehouse().getName() : null));
    }

    @Override
    public List<SupplyLot> findAllSupplyLots() {
        return supplyLotRepository.findAll();
    }

    @Override
    public Optional<SupplyLot> findSupplyLotByIdWithDetails(Integer lotId) {
        if (lotId == null) {
            return Optional.empty();
        }
        return supplyLotRepository.findByIdWithDetails(lotId);
    }

    @Override
    public boolean existsSupplyLotById(Integer lotId) {
        if (lotId == null) {
            return false;
        }
        return supplyLotRepository.existsById(lotId);
    }

    @Override
    public List<InventoryBalance> findAllInventoryBalancesWithDetails() {
        return inventoryBalanceRepository.findAllWithDetails();
    }

    @Override
    public List<InventoryBalance> findInventoryBalancesBySupplyLotId(Integer lotId) {
        if (lotId == null) {
            return List.of();
        }
        return inventoryBalanceRepository.findDetailedBySupplyLotId(lotId);
    }

    @Override
    public List<StockMovement> findStockMovementsBySupplyLotId(Integer lotId) {
        if (lotId == null) {
            return List.of();
        }
        return stockMovementRepository.findBySupplyLotIdOrderByMovementDateDesc(lotId);
    }
}
