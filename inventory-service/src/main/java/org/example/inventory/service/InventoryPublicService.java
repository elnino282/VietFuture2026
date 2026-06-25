package org.example.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.dto.request.CreateWarehouseRequest;
import org.example.inventory.dto.request.RecordStockMovementRequest;
import org.example.inventory.dto.request.UpdateWarehouseRequest;
import org.example.inventory.dto.response.OnHandRowResponse;
import org.example.inventory.dto.response.StockLocationResponse;
import org.example.inventory.dto.response.StockMovementResponse;
import org.example.inventory.dto.response.WarehouseResponse;
import org.example.inventory.entity.InventoryBalance;
import org.example.inventory.entity.StockLocation;
import org.example.inventory.entity.StockMovement;
import org.example.inventory.entity.SupplyItem;
import org.example.inventory.entity.SupplyLot;
import org.example.inventory.entity.Warehouse;
import org.example.inventory.enums.StockMovementType;
import org.example.inventory.exception.AppException;
import org.example.inventory.exception.ErrorCode;
import org.example.inventory.repository.InventoryBalanceRepository;
import org.example.inventory.repository.ProductWarehouseLotRepository;
import org.example.inventory.repository.StockLocationRepository;
import org.example.inventory.repository.StockMovementRepository;
import org.example.inventory.repository.SupplyItemRepository;
import org.example.inventory.repository.SupplyLotRepository;
import org.example.inventory.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InventoryPublicService {

    static final String INPUT_WAREHOUSE_TYPE = "INPUT";
    static final String OUTPUT_WAREHOUSE_TYPE = "OUTPUT";

    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    SupplyLotRepository supplyLotRepository;
    SupplyItemRepository supplyItemRepository;
    StockMovementRepository stockMovementRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    ProductWarehouseLotRepository productWarehouseLotRepository;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getMyWarehouses(String type) {
        if (type != null && !type.isBlank()) {
            return warehouseRepository.findAllByTypeIgnoreCase(normalizeWarehouseType(type)).stream()
                    .map(this::toWarehouseResponse)
                    .toList();
        }
        return warehouseRepository.findAll().stream()
                .map(this::toWarehouseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Integer warehouseId) {
        return toWarehouseResponse(getWarehouse(warehouseId));
    }

    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        String warehouseName = normalizeWarehouseName(request.getName());
        String warehouseType = normalizeWarehouseType(request.getType());

        if (warehouseRepository.existsByFarmIdAndNameIgnoreCaseAndTypeIgnoreCase(
                request.getFarmId(), warehouseName, warehouseType)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Warehouse warehouse = Warehouse.builder()
                .farmId(request.getFarmId())
                .name(warehouseName)
                .type(warehouseType)
                .provinceId(request.getProvinceId())
                .wardId(request.getWardId())
                .build();

        return toWarehouseResponse(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse updateWarehouse(Integer warehouseId, UpdateWarehouseRequest request) {
        Warehouse warehouse = getWarehouse(warehouseId);
        String warehouseName = normalizeWarehouseName(request.getName());
        String warehouseType = normalizeWarehouseType(warehouse.getType());

        if (!warehouse.getFarmId().equals(request.getFarmId())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (warehouseRepository.existsByFarmIdAndNameIgnoreCaseAndTypeIgnoreCaseAndIdNot(
                request.getFarmId(), warehouseName, warehouseType, warehouseId)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        warehouse.setName(warehouseName);
        warehouse.setProvinceId(request.getProvinceId());
        warehouse.setWardId(request.getWardId());

        return toWarehouseResponse(warehouseRepository.save(warehouse));
    }

    public void deleteWarehouse(Integer warehouseId) {
        Warehouse warehouse = getWarehouse(warehouseId);
        boolean hasDependencies = stockLocationRepository.existsByWarehouseId(warehouseId)
                || stockMovementRepository.existsByWarehouseId(warehouseId)
                || inventoryBalanceRepository.existsByWarehouseId(warehouseId)
                || productWarehouseLotRepository.existsByWarehouseId(warehouseId);
        if (hasDependencies) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        warehouseRepository.delete(warehouse);
    }

    @Transactional(readOnly = true)
    public List<StockLocationResponse> getLocationsByWarehouse(Integer warehouseId) {
        getWarehouse(warehouseId);
        return stockLocationRepository.findAllByWarehouseId(warehouseId).stream()
                .map(this::toLocationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OnHandRowResponse> getOnHandList(
            Integer warehouseId,
            Integer locationId,
            Integer lotId,
            String q,
            Pageable pageable) {
        getWarehouse(warehouseId);
        if (locationId != null) {
            ensureLocationBelongsToWarehouse(getLocation(locationId), warehouseId);
        }

        Page<InventoryBalance> page = inventoryBalanceRepository.searchOnHand(
                warehouseId,
                locationId,
                lotId,
                normalizeQuery(q),
                pageable);
        List<OnHandRowResponse> items = page.getContent().stream()
                .map(this::toOnHandRowResponse)
                .toList();
        return PageResponse.of(page, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getMovements(
            Integer warehouseId,
            String type,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        getWarehouse(warehouseId);
        StockMovementType movementType = parseMovementType(type);
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : null;

        Page<StockMovement> page = stockMovementRepository.searchByWarehouse(
                warehouseId,
                movementType,
                fromDate,
                toDate,
                pageable);
        List<StockMovementResponse> items = page.getContent().stream()
                .map(this::toMovementResponse)
                .toList();
        return PageResponse.of(page, items);
    }

    public StockMovementResponse recordMovement(RecordStockMovementRequest request) {
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        SupplyLot lot = getSupplyLot(request.getSupplyLotId());
        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = getLocation(request.getLocationId());
            ensureLocationBelongsToWarehouse(location, warehouse.getId());
        }

        StockMovementType type = parseMovementType(request.getMovementType());
        if (type == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (type != StockMovementType.ADJUST && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (type == StockMovementType.OUT) {
            quantity = quantity.abs();
        }

        applyInventoryBalance(lot, warehouse, location, type, quantity);

        StockMovement movement = StockMovement.builder()
                .supplyLotId(lot.getId())
                .warehouseId(warehouse.getId())
                .locationId(location != null ? location.getId() : null)
                .movementType(type)
                .quantity(quantity)
                .movementDate(LocalDateTime.now())
                .seasonId(request.getSeasonId())
                .taskId(request.getTaskId())
                .note(request.getNote())
                .build();

        return toMovementResponse(stockMovementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public BigDecimal getOnHandQuantity(Integer supplyLotId, Integer warehouseId, Integer locationId) {
        getWarehouse(warehouseId);
        getSupplyLot(supplyLotId);
        if (locationId != null) {
            ensureLocationBelongsToWarehouse(getLocation(locationId), warehouseId);
        }
        if (locationId != null) {
            return inventoryBalanceRepository.findByLotAndWarehouseAndLocation(supplyLotId, warehouseId, locationId)
                    .map(InventoryBalance::getQuantity)
                    .orElse(BigDecimal.ZERO);
        }
        return inventoryBalanceRepository.findAll().stream()
                .filter(balance -> supplyLotId.equals(balance.getSupplyLotId()))
                .filter(balance -> warehouseId.equals(balance.getWarehouseId()))
                .map(InventoryBalance::getQuantity)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyInventoryBalance(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location,
            StockMovementType type,
            BigDecimal quantity) {
        if (type == StockMovementType.IN) {
            upsertBalance(lot.getId(), warehouse.getId(), location != null ? location.getId() : null, quantity);
            return;
        }

        if (type == StockMovementType.ADJUST) {
            applyAdjustment(lot.getId(), warehouse.getId(), location != null ? location.getId() : null, quantity);
            return;
        }

        if (location != null) {
            deductFromSpecificLocation(lot.getId(), warehouse.getId(), location.getId(), quantity);
            return;
        }
        deductAcrossWarehouse(lot.getId(), warehouse.getId(), quantity);
    }

    private void upsertBalance(Integer lotId, Integer warehouseId, Integer locationId, BigDecimal quantity) {
        InventoryBalance balance = inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(
                        lotId, warehouseId, locationId)
                .orElseGet(() -> InventoryBalance.builder()
                        .supplyLotId(lotId)
                        .warehouseId(warehouseId)
                        .locationId(locationId)
                        .quantity(BigDecimal.ZERO)
                        .build());
        BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        balance.setQuantity(current.add(quantity));
        if (balance.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        inventoryBalanceRepository.save(balance);
    }

    private void applyAdjustment(Integer lotId, Integer warehouseId, Integer locationId, BigDecimal delta) {
        if (locationId != null || delta.compareTo(BigDecimal.ZERO) > 0) {
            upsertBalance(lotId, warehouseId, locationId, delta);
            return;
        }
        deductAcrossWarehouse(lotId, warehouseId, delta.abs());
    }

    private void deductFromSpecificLocation(Integer lotId, Integer warehouseId, Integer locationId, BigDecimal quantity) {
        InventoryBalance balance = inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(
                        lotId, warehouseId, locationId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        if (current.compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        balance.setQuantity(current.subtract(quantity));
        inventoryBalanceRepository.save(balance);
    }

    private void deductAcrossWarehouse(Integer lotId, Integer warehouseId, BigDecimal quantity) {
        List<InventoryBalance> balances = inventoryBalanceRepository.findAllPositiveByLotAndWarehouseWithLock(
                lotId, warehouseId);
        BigDecimal available = balances.stream()
                .map(InventoryBalance::getQuantity)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (available.compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        BigDecimal remaining = quantity;
        for (InventoryBalance balance : balances) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
            BigDecimal deducted = current.min(remaining);
            balance.setQuantity(current.subtract(deducted));
            inventoryBalanceRepository.save(balance);
            remaining = remaining.subtract(deducted);
        }
    }

    Warehouse getWarehouse(Integer warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    StockLocation getLocation(Integer locationId) {
        return stockLocationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    SupplyLot getSupplyLot(Integer supplyLotId) {
        return supplyLotRepository.findById(supplyLotId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    SupplyItem getSupplyItem(Integer supplyItemId) {
        return supplyItemRepository.findById(supplyItemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    void ensureLocationBelongsToWarehouse(StockLocation location, Integer warehouseId) {
        if (location == null || !warehouseId.equals(location.getWarehouseId())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .type(warehouse.getType())
                .farmId(warehouse.getFarmId())
                .provinceId(warehouse.getProvinceId())
                .wardId(warehouse.getWardId())
                .build();
    }

    StockLocationResponse toLocationResponse(StockLocation location) {
        return StockLocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouseId())
                .zone(location.getZone())
                .aisle(location.getAisle())
                .shelf(location.getShelf())
                .bin(location.getBin())
                .label(buildLocationLabel(location))
                .build();
    }

    StockMovementResponse toMovementResponse(StockMovement movement) {
        SupplyLot lot = supplyLotRepository.findById(movement.getSupplyLotId()).orElse(null);
        SupplyItem item = lot != null ? supplyItemRepository.findById(lot.getSupplyItemId()).orElse(null) : null;
        Warehouse warehouse = warehouseRepository.findById(movement.getWarehouseId()).orElse(null);
        StockLocation location = movement.getLocationId() != null
                ? stockLocationRepository.findById(movement.getLocationId()).orElse(null)
                : null;

        return StockMovementResponse.builder()
                .id(movement.getId())
                .supplyLotId(movement.getSupplyLotId())
                .batchCode(lot != null ? lot.getBatchCode() : null)
                .supplyItemName(item != null ? item.getName() : null)
                .unit(item != null ? item.getUnit() : null)
                .warehouseId(movement.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .locationId(movement.getLocationId())
                .locationLabel(location != null ? buildLocationLabel(location) : null)
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .quantity(movement.getQuantity())
                .movementDate(movement.getMovementDate())
                .seasonId(movement.getSeasonId())
                .taskId(movement.getTaskId())
                .note(movement.getNote())
                .build();
    }

    private OnHandRowResponse toOnHandRowResponse(InventoryBalance balance) {
        Warehouse warehouse = warehouseRepository.findById(balance.getWarehouseId()).orElse(null);
        StockLocation location = balance.getLocationId() != null
                ? stockLocationRepository.findById(balance.getLocationId()).orElse(null)
                : null;
        SupplyLot lot = supplyLotRepository.findById(balance.getSupplyLotId()).orElse(null);
        SupplyItem item = lot != null ? supplyItemRepository.findById(lot.getSupplyItemId()).orElse(null) : null;

        return OnHandRowResponse.builder()
                .warehouseId(balance.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .locationId(balance.getLocationId())
                .locationLabel(location != null ? buildLocationLabel(location) : "Any Location")
                .supplyLotId(balance.getSupplyLotId())
                .batchCode(lot != null ? lot.getBatchCode() : null)
                .supplyItemName(item != null ? item.getName() : null)
                .unit(item != null ? item.getUnit() : null)
                .expiryDate(lot != null ? lot.getExpiryDate() : null)
                .lotStatus(lot != null ? lot.getStatus() : null)
                .onHandQuantity(balance.getQuantity())
                .build();
    }

    private String normalizeWarehouseName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return name.trim();
    }

    private String normalizeWarehouseType(String type) {
        if (type == null || type.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!INPUT_WAREHOUSE_TYPE.equals(normalized) && !OUTPUT_WAREHOUSE_TYPE.equals(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private StockMovementType parseMovementType(String type) {
        try {
            return StockMovementType.fromCode(type);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String normalizeQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    String buildLocationLabel(StockLocation location) {
        StringBuilder sb = new StringBuilder();
        appendLabelPart(sb, location.getZone());
        appendLabelPart(sb, location.getAisle());
        appendLabelPart(sb, location.getShelf());
        appendLabelPart(sb, location.getBin());
        return sb.length() > 0 ? sb.toString() : "Location " + location.getId();
    }

    private void appendLabelPart(StringBuilder label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (label.length() > 0) {
            label.append("-");
        }
        label.append(value.trim());
    }
}
