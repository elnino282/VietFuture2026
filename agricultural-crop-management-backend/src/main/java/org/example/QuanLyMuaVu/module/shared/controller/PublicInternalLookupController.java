package org.example.QuanLyMuaVu.module.shared.controller;

import lombok.*;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.repository.IncidentRepository;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyItemRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryCommandPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/lookup")
@RequiredArgsConstructor
public class PublicInternalLookupController {

    private final IncidentRepository incidentRepository;
    private final ExpenseRepository expenseRepository;
    private final SupplyLotRepository supplyLotRepository;
    private final SupplyItemRepository supplyItemRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;
    private final ProductWarehouseLotRepository productWarehouseLotRepository;
    private final InventoryCommandPort inventoryCommandPort;
    private final InventoryQueryPort inventoryQueryPort;

    @PostMapping("/notifications")
    public void createNotification(@RequestBody CreateNotificationRequest request) {
        notificationService.createNotification(request.getUserId(), request.getTitle(), request.getMessage(), request.getLink());
    }

    @GetMapping("/incidents/{incidentId}/validate-season/{seasonId}")
    public ValidationResultDto validateIncidentSeason(@PathVariable Integer incidentId, @PathVariable Integer seasonId) {
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("INCIDENT_NOT_FOUND")
                    .errorMessage("Incident not found")
                    .build();
        }
        Integer incidentSeasonId = incident.getSeasonId() != null
                ? incident.getSeasonId()
                : incident.getSeason() != null ? incident.getSeason().getId() : null;
        if (incidentSeasonId == null || !incidentSeasonId.equals(seasonId)) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("DISEASE_REFERENCE_SEASON_MISMATCH")
                    .errorMessage("Incident season mismatch")
                    .build();
        }
        return ValidationResultDto.builder().valid(true).build();
    }

    @GetMapping("/expenses/{expenseId}/validate-season/{seasonId}")
    public ValidationResultDto validateExpenseSeason(@PathVariable Integer expenseId, @PathVariable Integer seasonId) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("EXPENSE_NOT_FOUND")
                    .errorMessage("Expense not found")
                    .build();
        }
        Integer expenseSeasonId = expense.getSeasonId() != null
                ? expense.getSeasonId()
                : expense.getSeason() != null ? expense.getSeason().getId() : null;
        if (expenseSeasonId == null || !expenseSeasonId.equals(seasonId)) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("DISEASE_REFERENCE_SEASON_MISMATCH")
                    .errorMessage("Expense season mismatch")
                    .build();
        }
        return ValidationResultDto.builder().valid(true).build();
    }

    @GetMapping("/supplies/validate-lot")
    public ValidationResultDto validateSupplyLot(
            @RequestParam Integer lotId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam List<Integer> farmIds) {

        SupplyLot supplyLot = supplyLotRepository.findById(lotId).orElse(null);
        if (supplyLot == null) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("SUPPLY_LOT_NOT_FOUND")
                    .errorMessage("Supply lot not found")
                    .build();
        }

        boolean hasBalance = inventoryBalanceRepository
                .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(lotId, farmIds);
        boolean hasMovements = stockMovementRepository
                .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(lotId, farmIds);
        if (!hasBalance && !hasMovements) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("FORBIDDEN")
                    .errorMessage("Supply lot not accessible for farms")
                    .build();
        }

        Integer lotSupplyItemId = supplyLot.getSupplyItem() != null ? supplyLot.getSupplyItem().getId() : null;
        if (itemId != null && lotSupplyItemId != null && !itemId.equals(lotSupplyItemId)) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("DISEASE_SUPPLY_ITEM_LOT_MISMATCH")
                    .errorMessage("Supply item lot mismatch")
                    .build();
        }

        return ValidationResultDto.builder()
                .valid(true)
                .resolvedItemId(lotSupplyItemId)
                .build();
    }

    @GetMapping("/supplies/validate-item")
    public ValidationResultDto validateSupplyItem(
            @RequestParam Integer itemId,
            @RequestParam List<Integer> farmIds) {

        if (!supplyItemRepository.existsById(itemId)) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("SUPPLY_ITEM_NOT_FOUND")
                    .errorMessage("Supply item not found")
                    .build();
        }

        boolean hasPositiveBalance = inventoryBalanceRepository
                .existsBySupplyLot_SupplyItem_IdAndWarehouse_Farm_IdInAndQuantityGreaterThan(
                        itemId,
                        farmIds,
                        BigDecimal.ZERO);
        if (!hasPositiveBalance) {
            return ValidationResultDto.builder()
                    .valid(false)
                    .errorCode("FORBIDDEN")
                    .errorMessage("Supply item not accessible for farms")
                    .build();
        }

        return ValidationResultDto.builder().valid(true).build();
    }

    @GetMapping("/supplies/items/{id}/name")
    public String getSupplyItemName(@PathVariable Integer id) {
        return supplyItemRepository.findById(id)
                .map(item -> item.getName())
                .orElse(null);
    }

    @GetMapping("/supplies/lots/{id}/batch-code")
    public String getSupplyLotBatchCode(@PathVariable Integer id) {
        return supplyLotRepository.findById(id)
                .map(lot -> lot.getBatchCode())
                .orElse(null);
    }

    // --- Inventory Bridge Endpoints for season-service ---

    @PostMapping("/inventory/receive-harvest")
    public ProductWarehouseLotDto receiveFromHarvest(
            @RequestParam Integer harvestId,
            @RequestParam Long actorUserId,
            @RequestBody org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest request) {
        ProductWarehouseLot lot = inventoryCommandPort.receiveFromHarvest(harvestId, actorUserId, request);
        return toLotDto(lot);
    }

    @GetMapping("/inventory/lots/by-seasons")
    public List<ProductWarehouseLotDto> findLotsBySeasonIds(@RequestParam List<Integer> seasonIds) {
        List<ProductWarehouseLot> lots = productWarehouseLotRepository.findAllBySeason_IdIn(seasonIds);
        return lots.stream().map(this::toLotDto).collect(Collectors.toList());
    }

    @GetMapping("/inventory/lots/by-harvest/{harvestId}")
    public ProductWarehouseLotDto findLotByHarvestId(@PathVariable Integer harvestId) {
        return productWarehouseLotRepository.findByHarvest_Id(harvestId)
                .map(this::toLotDto)
                .orElse(null);
    }

    @GetMapping("/inventory/lots/by-harvests")
    public List<ProductWarehouseLotDto> findLotsByHarvestIds(@RequestParam List<Integer> harvestIds) {
        List<ProductWarehouseLot> lots = productWarehouseLotRepository.findAllByHarvest_IdIn(harvestIds);
        return lots.stream().map(this::toLotDto).collect(Collectors.toList());
    }

    @PostMapping("/inventory/lots/{lotId}/sync")
    public ProductWarehouseLotDto syncLinkedLotFromHarvest(@PathVariable Integer lotId, @RequestBody SyncLotRequest request) {
        ProductWarehouseLot lot = productWarehouseLotRepository.findById(lotId).orElse(null);
        if (lot == null) return null;
        lot.setHarvestedAt(request.getHarvestedAt());
        lot.setInitialQuantity(request.getInitialQuantity());
        lot.setOnHandQuantity(request.getOnHandQuantity());
        lot.setGrade(request.getGrade());
        lot.setQualityStatus(request.getQualityStatus());
        lot.setNote(request.getNote());
        if (request.getStatus() != null) {
            lot.setStatus(org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.valueOf(request.getStatus()));
        }
        lot = productWarehouseLotRepository.save(lot);
        return toLotDto(lot);
    }

    @GetMapping("/inventory/exists-by-harvest/{harvestId}")
    public Boolean existsProductWarehouseLotByHarvestId(@PathVariable Integer harvestId) {
        return inventoryQueryPort.existsProductWarehouseLotByHarvestId(harvestId);
    }

    @GetMapping("/inventory/stock-context")
    public HarvestStockContextDto findHarvestStockContext(
            @RequestParam Integer farmId,
            @RequestParam Integer warehouseId,
            @RequestParam String productName,
            @RequestParam String lotCode) {
        return inventoryQueryPort.findHarvestStockContext(farmId, warehouseId, productName, lotCode)
                .map(view -> HarvestStockContextDto.builder()
                        .warehouseName(view.warehouseName())
                        .matchingLots(view.matchingLots())
                        .onHandQuantity(view.onHandQuantity())
                        .unit(view.unit())
                        .build())
                .orElse(null);
    }

    private ProductWarehouseLotDto toLotDto(ProductWarehouseLot lot) {
        if (lot == null) return null;
        return ProductWarehouseLotDto.builder()
                .id(lot.getId())
                .lotCode(lot.getLotCode())
                .productId(lot.getProductId())
                .productName(lot.getProductName())
                .productVariant(lot.getProductVariant())
                .harvestId(lot.getHarvest() != null ? lot.getHarvest().getId() : null)
                .warehouseId(lot.getWarehouse() != null ? lot.getWarehouse().getId() : null)
                .locationId(lot.getLocation() != null ? lot.getLocation().getId() : null)
                .harvestedAt(lot.getHarvestedAt())
                .unit(lot.getUnit())
                .initialQuantity(lot.getInitialQuantity())
                .onHandQuantity(lot.getOnHandQuantity())
                .grade(lot.getGrade())
                .qualityStatus(lot.getQualityStatus())
                .note(lot.getNote())
                .status(lot.getStatus() != null ? lot.getStatus().name() : null)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductWarehouseLotDto {
        private Integer id;
        private String lotCode;
        private Integer productId;
        private String productName;
        private String productVariant;
        private Integer harvestId;
        private Integer warehouseId;
        private Integer locationId;
        private LocalDate harvestedAt;
        private String unit;
        private BigDecimal initialQuantity;
        private BigDecimal onHandQuantity;
        private String grade;
        private String qualityStatus;
        private String note;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncLotRequest {
        private LocalDate harvestedAt;
        private BigDecimal initialQuantity;
        private BigDecimal onHandQuantity;
        private String grade;
        private String qualityStatus;
        private String note;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HarvestStockContextDto {
        private String warehouseName;
        private Long matchingLots;
        private BigDecimal onHandQuantity;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResultDto {
        private boolean valid;
        private String errorCode;
        private String errorMessage;
        private Integer resolvedItemId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNotificationRequest {
        private Long userId;
        private String title;
        private String message;
        private String link;
    }
}
