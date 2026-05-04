package org.example.QuanLyMuaVu.module.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.dto.request.CreateWarehouseRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.RecordStockMovementRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateWarehouseRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.OnHandRowResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockLocationResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockMovementResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.WarehouseResponse;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockLocationRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InventoryService {

    static final String INPUT_WAREHOUSE_TYPE = "INPUT";
    static final String OUTPUT_WAREHOUSE_TYPE = "OUTPUT";

    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    SupplyLotRepository supplyLotRepository;
    StockMovementRepository stockMovementRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    ProductWarehouseLotRepository productWarehouseLotRepository;
    FarmQueryPort farmQueryPort;
    SeasonQueryPort seasonQueryPort;
    TaskQueryPort taskQueryPort;
    FarmAccessPort farmAccessService;
    AuditLogService auditLogService;

    // ============================================
    // GET MY WAREHOUSES
    // ============================================
    public List<WarehouseResponse> getMyWarehouses(String type) {
        List<Integer> farmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (farmIds.isEmpty()) {
            return List.of();
        }
        List<org.example.QuanLyMuaVu.module.farm.entity.Farm> myFarms = farmQueryPort.findFarmsByIds(farmIds);
        List<Warehouse> warehouses;
        if (type != null && !type.isBlank()) {
            String normalizedType = normalizeWarehouseType(type);
            warehouses = warehouseRepository.findByFarmInAndTypeIgnoreCase(myFarms, normalizedType);
        } else {
            warehouses = warehouseRepository.findByFarmIn(myFarms);
        }
        return warehouses.stream()
                .map(this::toWarehouseResponse)
                .collect(Collectors.toList());
    }

    public WarehouseResponse getWarehouseById(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);
        return toWarehouseResponse(warehouse);
    }

    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        String warehouseName = normalizeWarehouseName(request.getName());
        String warehouseType = normalizeWarehouseType(request.getType());

        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = farmQueryPort.findFarmById(request.getFarmId())
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessFarm(farm);

        if (warehouseRepository.existsByFarmAndNameIgnoreCaseAndTypeIgnoreCase(farm, warehouseName, warehouseType)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        org.example.QuanLyMuaVu.module.farm.entity.Province province = resolveProvince(request.getProvinceId());
        org.example.QuanLyMuaVu.module.farm.entity.Ward ward = resolveWard(request.getWardId(), province);

        Warehouse warehouse = Warehouse.builder()
                .name(warehouseName)
                .type(warehouseType)
                .farm(farm)
                .province(province)
                .ward(ward)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        return toWarehouseResponse(saved);
    }

    public WarehouseResponse updateWarehouse(Integer warehouseId, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        String warehouseName = normalizeWarehouseName(request.getName());

        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = farmQueryPort.findFarmById(request.getFarmId())
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessFarm(farm);
        if (warehouse.getFarm() == null
                || warehouse.getFarm().getId() == null
                || !warehouse.getFarm().getId().equals(farm.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String warehouseType = normalizeWarehouseType(warehouse.getType());
        if (warehouseRepository.existsByFarmAndNameIgnoreCaseAndTypeIgnoreCaseAndIdNot(
                farm,
                warehouseName,
                warehouseType,
                warehouseId)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        org.example.QuanLyMuaVu.module.farm.entity.Province province = resolveProvince(request.getProvinceId());
        org.example.QuanLyMuaVu.module.farm.entity.Ward ward = resolveWard(request.getWardId(), province);

        warehouse.setName(warehouseName);
        warehouse.setProvince(province);
        warehouse.setWard(ward);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toWarehouseResponse(saved);
    }

    public void deleteWarehouse(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        boolean hasDependencies = stockLocationRepository.existsByWarehouse_Id(warehouseId)
                || stockMovementRepository.existsByWarehouse_Id(warehouseId)
                || inventoryBalanceRepository.existsByWarehouse_Id(warehouseId)
                || productWarehouseLotRepository.existsByWarehouse_Id(warehouseId);
        if (hasDependencies) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        warehouseRepository.delete(warehouse);
    }

    // ============================================
    // GET LOCATIONS BY WAREHOUSE
    // ============================================
    public List<StockLocationResponse> getLocationsByWarehouse(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        List<StockLocation> locations = stockLocationRepository.findAllByWarehouse(warehouse);
        return locations.stream()
                .map(this::toStockLocationResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET ON-HAND LIST (Paginated)
    // ============================================
    public PageResponse<OnHandRowResponse> getOnHandList(Integer warehouseId, Integer locationId,
            Integer lotId, String q, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        StockLocation location = null;
        if (locationId != null) {
            location = stockLocationRepository.findById(locationId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // Get all distinct supply lot IDs with movements at this warehouse/location
        List<Integer> lotIds = stockMovementRepository.findDistinctSupplyLotIdsByWarehouse(warehouse, location);

        if (lotIds.isEmpty()) {
            return createEmptyPageResponse(pageable);
        }

        // Filter by specific lot if provided
        if (lotId != null) {
            if (!lotIds.contains(lotId)) {
                return createEmptyPageResponse(pageable);
            }
            lotIds = List.of(lotId);
        }

        // Build on-hand rows
        List<OnHandRowResponse> allRows = new ArrayList<>();
        for (Integer lid : lotIds) {
            SupplyLot lot = supplyLotRepository.findById(lid).orElse(null);
            if (lot == null)
                continue;

            // Filter by search query if provided
            if (q != null && !q.isBlank()) {
                String searchLower = q.toLowerCase();
                boolean matches = false;
                if (lot.getBatchCode() != null && lot.getBatchCode().toLowerCase().contains(searchLower)) {
                    matches = true;
                }
                if (lot.getSupplyItem() != null && lot.getSupplyItem().getName() != null
                        && lot.getSupplyItem().getName().toLowerCase().contains(searchLower)) {
                    matches = true;
                }
                if (!matches)
                    continue;
            }

            BigDecimal onHand = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);

            // Only include rows with positive on-hand
            if (onHand.compareTo(BigDecimal.ZERO) > 0) {
                OnHandRowResponse row = OnHandRowResponse.builder()
                        .warehouseId(warehouse.getId())
                        .warehouseName(warehouse.getName())
                        .locationId(location != null ? location.getId() : null)
                        .locationLabel(location != null ? buildLocationLabel(location) : "Any Location")
                        .supplyLotId(lot.getId())
                        .batchCode(lot.getBatchCode())
                        .supplyItemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                        .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                        .expiryDate(lot.getExpiryDate())
                        .lotStatus(lot.getStatus())
                        .onHandQuantity(onHand)
                        .build();
                allRows.add(row);
            }
        }

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRows.size());

        if (start >= allRows.size()) {
            return createEmptyPageResponse(pageable);
        }

        List<OnHandRowResponse> pagedRows = allRows.subList(start, end);

        PageResponse<OnHandRowResponse> response = new PageResponse<>();
        response.setItems(pagedRows);
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(allRows.size());
        response.setTotalPages((int) Math.ceil((double) allRows.size() / pageable.getPageSize()));
        return response;
    }

    // ============================================
    // GET MOVEMENTS (Paginated History)
    // ============================================
    public PageResponse<StockMovementResponse> getMovements(Integer warehouseId, String type,
            LocalDate from, LocalDate to, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        StockMovementType movementType = null;
        if (type != null && !type.isBlank()) {
            movementType = StockMovementType.fromCode(type);
        }

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(23, 59, 59) : null;

        Page<StockMovement> movementsPage = stockMovementRepository.findByWarehouseWithFilters(
                warehouse, movementType, fromDateTime, toDateTime, pageable);

        List<StockMovementResponse> items = movementsPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(movementsPage, items);
    }

    // ============================================
    // RECORD MOVEMENT (Enhanced with validations)
    // ============================================
    public StockMovementResponse recordMovement(RecordStockMovementRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        SupplyLot lot = supplyLotRepository.findById(request.getSupplyLotId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = stockLocationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        StockMovementType type = StockMovementType.fromCode(request.getMovementType());
        if (type == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        ensureSupplyLotOwnership(lot, type);

        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        // ========== VALIDATION: ADJUST requires note ==========
        if (type == StockMovementType.ADJUST) {
            if (request.getNote() == null || request.getNote().isBlank()) {
                throw new AppException(ErrorCode.ADJUST_NOTE_REQUIRED);
            }
        }

        // ========== VALIDATION: OUT requires season and lot status ==========
        if (type == StockMovementType.OUT) {
            if (request.getSeasonId() == null) {
                throw new AppException(ErrorCode.OUT_SEASON_REQUIRED);
            }
            if (!"IN_STOCK".equals(lot.getStatus())) {
                throw new AppException(ErrorCode.LOT_NOT_IN_STOCK);
            }
        }

        org.example.QuanLyMuaVu.module.season.entity.Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonQueryPort.findSeasonById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        }

        var task = request.getTaskId() != null
                ? taskQueryPort.findTaskById(request.getTaskId())
                        .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND))
                : null;

        if (task != null) {
            if (task.getSeason() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (season != null && !task.getSeason().getId().equals(season.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (season == null) {
                season = task.getSeason();
            }
        }

        // ========== VALIDATION: org.example.QuanLyMuaVu.module.season.entity.Season and warehouse must be same farm (for OUT)
        // ==========
        if (type == StockMovementType.OUT && season != null) {
            if (season.getPlot() == null || season.getPlot().getFarm() == null
                    || !season.getPlot().getFarm().getId().equals(warehouse.getFarm().getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // Keep movement log and inventory balance update in the same transaction boundary.
        applyInventoryBalance(lot, warehouse, location, type, quantity);

        StockMovement movement = StockMovement.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .location(location)
                .movementType(type)
                .quantity(quantity)
                .movementDate(LocalDateTime.now())
                .season(season)
                .task(task)
                .note(request.getNote())
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        if (type == StockMovementType.ADJUST) {
            java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("movementId", saved.getId());
            snapshot.put("warehouseId", warehouse.getId());
            snapshot.put("supplyLotId", lot.getId());
            snapshot.put("quantity", quantity);
            snapshot.put("movementType", type.name());
            snapshot.put("note", request.getNote());

            auditLogService.logModuleOperation(
                    "INVENTORY",
                    "STOCK_MOVEMENT",
                    saved.getId(),
                    "INVENTORY_ADJUSTED",
                    resolveAuditActor(),
                    snapshot,
                    request.getNote(),
                    null);
        }
        return toResponse(saved);
    }

    // ============================================
    // GET ON-HAND QUANTITY (Simple endpoint)
    // ============================================
    public BigDecimal getOnHandQuantity(Integer supplyLotId, Integer warehouseId, Integer locationId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        SupplyLot lot = supplyLotRepository.findById(supplyLotId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        StockLocation location = null;
        if (locationId != null) {
            location = stockLocationRepository.findById(locationId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        }

        BigDecimal fromBalance = calculateOnHandFromBalance(lot, warehouse, location);
        if (fromBalance.compareTo(BigDecimal.ZERO) > 0) {
            return fromBalance;
        }
        return stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private String normalizeWarehouseName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        String normalized = name.trim();
        if (normalized.length() > 150) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
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

    private org.example.QuanLyMuaVu.module.farm.entity.Province resolveProvince(Integer provinceId) {
        if (provinceId == null) {
            return null;
        }
        return farmQueryPort.findProvinceById(provinceId)
                .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));
    }

    private org.example.QuanLyMuaVu.module.farm.entity.Ward resolveWard(Integer wardId, org.example.QuanLyMuaVu.module.farm.entity.Province province) {
        if (wardId == null) {
            return null;
        }
        org.example.QuanLyMuaVu.module.farm.entity.Ward ward = farmQueryPort.findWardById(wardId)
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND));
        if (province == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (ward.getProvince() == null || ward.getProvince().getId() == null
                || !ward.getProvince().getId().equals(province.getId())) {
            throw new AppException(ErrorCode.WARD_NOT_IN_PROVINCE);
        }
        return ward;
    }

    private void ensureWarehouseOwnership(Warehouse warehouse) {
        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = warehouse.getFarm();
        if (farm == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        farmAccessService.assertCurrentUserCanAccessFarm(farm);
    }

    private void ensureSupplyLotOwnership(SupplyLot lot, StockMovementType type) {
        if (lot == null || lot.getId() == null) {
            throw new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND);
        }

        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (accessibleFarmIds.isEmpty()) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        boolean hasBalanceInCurrentFarms = inventoryBalanceRepository
                .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(lot.getId(), accessibleFarmIds);
        if (hasBalanceInCurrentFarms) {
            return;
        }

        boolean hasAnyBalance = inventoryBalanceRepository.existsBySupplyLot_Id(lot.getId());
        if (!hasAnyBalance) {
            boolean hasMovementInCurrentFarms = stockMovementRepository
                    .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(lot.getId(), accessibleFarmIds);
            if (hasMovementInCurrentFarms) {
                return;
            }

            boolean hasAnyMovement = stockMovementRepository.existsBySupplyLot_Id(lot.getId());
            if (!hasAnyMovement && type == StockMovementType.IN) {
                return;
            }
        }

        throw new AppException(ErrorCode.FORBIDDEN);
    }

    private void applyInventoryBalance(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location,
            StockMovementType type,
            BigDecimal quantity) {
        if (type == StockMovementType.IN || type == StockMovementType.ADJUST) {
            upsertBalance(lot, warehouse, location, quantity);
            return;
        }

        if (type == StockMovementType.OUT) {
            if (location != null) {
                deductFromSpecificLocation(lot, warehouse, location, quantity);
                return;
            }
            deductAcrossWarehouse(lot, warehouse, quantity);
        }
    }

    private void upsertBalance(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location,
            BigDecimal quantity) {
        InventoryBalance balance = inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(lot, warehouse, location)
                .orElseGet(() -> InventoryBalance.builder()
                        .supplyLot(lot)
                        .warehouse(warehouse)
                        .location(location)
                        .quantity(BigDecimal.ZERO)
                        .build());

        BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        balance.setQuantity(current.add(quantity));
        inventoryBalanceRepository.save(balance);
    }

    private void deductFromSpecificLocation(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location,
            BigDecimal quantity) {
        InventoryBalance balance = inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(lot, warehouse, location)
                .orElseGet(() -> bootstrapBalanceFromMovements(lot, warehouse, location));

        BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        if (current.compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        balance.setQuantity(current.subtract(quantity));
        inventoryBalanceRepository.save(balance);
    }

    private void deductAcrossWarehouse(
            SupplyLot lot,
            Warehouse warehouse,
            BigDecimal quantity) {
        List<InventoryBalance> balances = new ArrayList<>(inventoryBalanceRepository.findAllByLotAndWarehouseWithLock(lot, warehouse));

        BigDecimal available = balances.stream()
                .map(InventoryBalance::getQuantity)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (available.compareTo(quantity) < 0) {
            BigDecimal fallback = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, null);
            fallback = fallback != null ? fallback : BigDecimal.ZERO;
            if (fallback.compareTo(quantity) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            BigDecimal delta = fallback.subtract(available);
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                InventoryBalance recoveryBalance = upsertRecoveryBalance(lot, warehouse, delta);
                balances.add(recoveryBalance);
            }
        }

        BigDecimal remaining = quantity;
        for (InventoryBalance balance : balances) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal current = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
            if (current.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal deducted = current.min(remaining);
            balance.setQuantity(current.subtract(deducted));
            inventoryBalanceRepository.save(balance);
            remaining = remaining.subtract(deducted);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private InventoryBalance bootstrapBalanceFromMovements(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location) {
        BigDecimal fallback = stockMovementRepository.calculateOnHandQuantity(lot, warehouse, location);
        fallback = fallback != null ? fallback : BigDecimal.ZERO;
        if (fallback.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        InventoryBalance balance = InventoryBalance.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .location(location)
                .quantity(fallback)
                .build();
        return inventoryBalanceRepository.save(balance);
    }

    private InventoryBalance upsertRecoveryBalance(
            SupplyLot lot,
            Warehouse warehouse,
            BigDecimal quantity) {
        InventoryBalance recovery = inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(lot, warehouse, null)
                .orElseGet(() -> InventoryBalance.builder()
                        .supplyLot(lot)
                        .warehouse(warehouse)
                        .location(null)
                        .quantity(BigDecimal.ZERO)
                        .build());

        BigDecimal current = recovery.getQuantity() != null ? recovery.getQuantity() : BigDecimal.ZERO;
        recovery.setQuantity(current.add(quantity));
        return inventoryBalanceRepository.save(recovery);
    }

    private BigDecimal calculateOnHandFromBalance(
            SupplyLot lot,
            Warehouse warehouse,
            StockLocation location) {
        if (location != null) {
            BigDecimal quantity = inventoryBalanceRepository.getCurrentQuantity(lot, warehouse, location);
            return quantity != null ? quantity : BigDecimal.ZERO;
        }
        BigDecimal quantity = inventoryBalanceRepository.sumQuantityByLotAndWarehouse(lot, warehouse);
        return quantity != null ? quantity : BigDecimal.ZERO;
    }

    private String buildLocationLabel(StockLocation location) {
        StringBuilder sb = new StringBuilder();
        if (location.getZone() != null)
            sb.append(location.getZone());
        if (location.getAisle() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getAisle());
        }
        if (location.getShelf() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getShelf());
        }
        if (location.getBin() != null) {
            if (sb.length() > 0)
                sb.append("-");
            sb.append(location.getBin());
        }
        return sb.length() > 0 ? sb.toString() : "Location " + location.getId();
    }

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .type(warehouse.getType())
                .farmId(warehouse.getFarm() != null ? warehouse.getFarm().getId() : null)
                .farmName(warehouse.getFarm() != null ? warehouse.getFarm().getName() : null)
                .provinceId(warehouse.getProvince() != null ? warehouse.getProvince().getId() : null)
                .wardId(warehouse.getWard() != null ? warehouse.getWard().getId() : null)
                .build();
    }

    private StockLocationResponse toStockLocationResponse(StockLocation location) {
        return StockLocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouse() != null ? location.getWarehouse().getId() : null)
                .zone(location.getZone())
                .aisle(location.getAisle())
                .shelf(location.getShelf())
                .bin(location.getBin())
                .label(buildLocationLabel(location))
                .build();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .supplyLotId(movement.getSupplyLot() != null ? movement.getSupplyLot().getId() : null)
                .batchCode(movement.getSupplyLot() != null ? movement.getSupplyLot().getBatchCode() : null)
                .supplyItemName(movement.getSupplyLot() != null && movement.getSupplyLot().getSupplyItem() != null
                        ? movement.getSupplyLot().getSupplyItem().getName()
                        : null)
                .unit(movement.getSupplyLot() != null && movement.getSupplyLot().getSupplyItem() != null
                        ? movement.getSupplyLot().getSupplyItem().getUnit()
                        : null)
                .warehouseId(movement.getWarehouse() != null ? movement.getWarehouse().getId() : null)
                .warehouseName(movement.getWarehouse() != null ? movement.getWarehouse().getName() : null)
                .locationId(movement.getLocation() != null ? movement.getLocation().getId() : null)
                .locationLabel(movement.getLocation() != null ? buildLocationLabel(movement.getLocation()) : null)
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .quantity(movement.getQuantity())
                .movementDate(movement.getMovementDate())
                .seasonId(movement.getSeason() != null ? movement.getSeason().getId() : null)
                .seasonName(movement.getSeason() != null ? movement.getSeason().getSeasonName() : null)
                .taskId(movement.getTask() != null ? movement.getTask().getId() : null)
                .taskTitle(movement.getTask() != null ? movement.getTask().getTitle() : null)
                .note(movement.getNote())
                .build();
    }

    private PageResponse<OnHandRowResponse> createEmptyPageResponse(Pageable pageable) {
        PageResponse<OnHandRowResponse> response = new PageResponse<>();
        response.setItems(List.of());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(0);
        response.setTotalPages(0);
        return response;
    }

    private String resolveAuditActor() {
        try {
            org.example.QuanLyMuaVu.module.identity.entity.User actor = farmAccessService.getCurrentUser();
            if (actor != null && actor.getUsername() != null && !actor.getUsername().isBlank()) {
                return actor.getUsername();
            }
        } catch (Exception ignored) {
            // Fallback handled below.
        }
        return "system";
    }
}
