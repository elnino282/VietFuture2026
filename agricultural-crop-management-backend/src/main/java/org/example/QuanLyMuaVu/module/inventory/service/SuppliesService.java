package org.example.QuanLyMuaVu.module.inventory.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.inventory.dto.request.RecordStockMovementRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.StockInRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockInResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockMovementResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplierResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyItemResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyLotResponse;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.Supplier;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockLocationRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplierRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyItemRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SuppliesService {

    SupplierRepository supplierRepository;
    SupplyItemRepository supplyItemRepository;
    SupplyLotRepository supplyLotRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    FarmAccessPort farmAccessService;
    InventoryService inventoryService;

    // ============================================
    // CATALOG: SUPPLIERS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getSuppliers(String q, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<Supplier> page = supplierRepository.searchByName(searchQuery, pageable);
        List<SupplierResponse> items = page.getContent().stream()
                .map(this::toSupplierResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    public SupplierResponse createSupplier(org.example.QuanLyMuaVu.module.inventory.dto.request.CreateSupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .licenseNo(request.getLicenseNo())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
        supplier = supplierRepository.save(supplier);
        return toSupplierResponse(supplier);
    }

    public SupplierResponse updateSupplier(Integer id, org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        
        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getLicenseNo() != null) {
            supplier.setLicenseNo(request.getLicenseNo());
        }
        if (request.getContactEmail() != null) {
            supplier.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            supplier.setContactPhone(request.getContactPhone());
        }
        
        supplier = supplierRepository.save(supplier);
        return toSupplierResponse(supplier);
    }

    public void deleteSupplier(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        supplierRepository.delete(supplier);
    }

    // ============================================
    // CATALOG: SUPPLY ITEMS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplyItemResponse> getSupplyItems(String q, Boolean restricted, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<SupplyItem> page = supplyItemRepository.searchItems(searchQuery, restricted, pageable);
        List<SupplyItemResponse> items = page.getContent().stream()
                .map(this::toSupplyItemResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplyItemResponse> getSupplyItemsForFarmIds(
            List<Integer> farmIds,
            String q,
            Boolean restricted,
            Pageable pageable) {
        List<InventoryBalance> balances = loadPositiveBalancesForFarmIds(farmIds);
        String searchQuery = q != null && !q.isBlank() ? q.trim().toLowerCase() : null;

        Map<Integer, SupplyItem> byId = new LinkedHashMap<>();
        for (InventoryBalance balance : balances) {
            SupplyLot lot = balance.getSupplyLot();
            SupplyItem item = lot != null ? lot.getSupplyItem() : null;
            if (item != null && item.getId() != null) {
                byId.putIfAbsent(item.getId(), item);
            }
        }

        List<SupplyItemResponse> filtered = byId.values().stream()
                .filter(item -> searchQuery == null
                        || (item.getName() != null && item.getName().toLowerCase().contains(searchQuery))
                        || (item.getActiveIngredient() != null
                                && item.getActiveIngredient().toLowerCase().contains(searchQuery)))
                .filter(item -> restricted == null || Boolean.TRUE.equals(item.getRestrictedFlag()) == restricted)
                .sorted(Comparator.comparing(
                        SupplyItem::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toSupplyItemResponse)
                .toList();

        return toPageResponse(filtered, pageable);
    }

    public SupplyItemResponse createSupplyItem(
            org.example.QuanLyMuaVu.module.inventory.dto.request.CreateSupplyItemRequest request) {
        String normalizedName = normalizeSupplyItemName(request.getName());
        if (supplyItemRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        SupplyItem item = SupplyItem.builder()
                .name(normalizedName)
                .activeIngredient(request.getActiveIngredient())
                .unit(normalizeRequired(request.getUnit()))
                .restrictedFlag(Boolean.TRUE.equals(request.getRestrictedFlag()))
                .build();
        item = supplyItemRepository.save(item);
        return toSupplyItemResponse(item);
    }

    public SupplyItemResponse updateSupplyItem(
            Integer id,
            org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateSupplyItemRequest request) {
        SupplyItem item = supplyItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_ITEM_NOT_FOUND));
        String normalizedName = normalizeSupplyItemName(request.getName());
        if (!item.getName().equalsIgnoreCase(normalizedName)
                && supplyItemRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        item.setName(normalizedName);
        item.setActiveIngredient(request.getActiveIngredient());
        item.setUnit(normalizeRequired(request.getUnit()));
        item.setRestrictedFlag(Boolean.TRUE.equals(request.getRestrictedFlag()));
        item = supplyItemRepository.save(item);
        return toSupplyItemResponse(item);
    }

    public void deleteSupplyItem(Integer id) {
        SupplyItem item = supplyItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_ITEM_NOT_FOUND));
        if (supplyLotRepository.existsBySupplyItem_Id(id)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        supplyItemRepository.delete(item);
    }

    // ============================================
    // CATALOG: SUPPLY LOTS
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<SupplyLotResponse> getSupplyLots(Integer itemId, Integer supplierId,
            String status, String q, Pageable pageable) {
        String searchQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        String statusFilter = (status != null && !status.isBlank()) ? status.trim() : null;
        Page<SupplyLot> page = supplyLotRepository.searchLots(itemId, supplierId, statusFilter, searchQuery, pageable);
        List<SupplyLotResponse> items = page.getContent().stream()
                .map(this::toSupplyLotResponse)
                .collect(Collectors.toList());
        return PageResponse.of(page, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplyLotResponse> getSupplyLotsForFarmIds(
            List<Integer> farmIds,
            Integer itemId,
            String status,
            String q,
            Pageable pageable) {
        List<InventoryBalance> balances = loadPositiveBalancesForFarmIds(farmIds);
        String searchQuery = q != null && !q.isBlank() ? q.trim().toLowerCase() : null;
        String statusFilter = status != null && !status.isBlank() ? status.trim() : null;

        Map<Integer, SupplyLot> byId = new LinkedHashMap<>();
        for (InventoryBalance balance : balances) {
            SupplyLot lot = balance.getSupplyLot();
            if (lot != null && lot.getId() != null) {
                byId.putIfAbsent(lot.getId(), lot);
            }
        }

        List<SupplyLotResponse> filtered = byId.values().stream()
                .filter(lot -> itemId == null
                        || (lot.getSupplyItem() != null && itemId.equals(lot.getSupplyItem().getId())))
                .filter(lot -> statusFilter == null || statusFilter.equals(lot.getStatus()))
                .filter(lot -> searchQuery == null
                        || (lot.getBatchCode() != null && lot.getBatchCode().toLowerCase().contains(searchQuery))
                        || (lot.getSupplyItem() != null
                                && lot.getSupplyItem().getName() != null
                                && lot.getSupplyItem().getName().toLowerCase().contains(searchQuery)))
                .sorted(Comparator.comparing(
                        SupplyLot::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSupplyLotResponse)
                .toList();

        return toPageResponse(filtered, pageable);
    }

    // ============================================
    // STOCK IN
    // ============================================
    public StockInResponse stockIn(StockInRequest request) {
        // 1. Validate warehouse ownership
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        ensureWarehouseOwnership(warehouse);

        // 2. Validate location if provided
        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = stockLocationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // 3. Load supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));

        // 4. Load supply item and check restricted flag
        SupplyItem supplyItem = supplyItemRepository.findById(request.getSupplyItemId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_ITEM_NOT_FOUND));

        // 5. If restricted item, require confirmation
        if (Boolean.TRUE.equals(supplyItem.getRestrictedFlag())) {
            if (!Boolean.TRUE.equals(request.getConfirmRestricted())) {
                throw new AppException(ErrorCode.RESTRICTED_CONFIRM_REQUIRED);
            }
        }

        // 6. Parse expiry date if provided
        LocalDate expiryDate = null;
        if (request.getExpiryDate() != null && !request.getExpiryDate().isBlank()) {
            expiryDate = LocalDate.parse(request.getExpiryDate());
        }

        // 7. Create SupplyLot
        SupplyLot lot = SupplyLot.builder()
                .supplyItem(supplyItem)
                .supplier(supplier)
                .batchCode(request.getBatchCode())
                .expiryDate(expiryDate)
                .status("IN_STOCK")
                .build();
        lot = supplyLotRepository.save(lot);

        // 8. Record movement through InventoryService to keep movement+balance consistency.
        RecordStockMovementRequest movementRequest = RecordStockMovementRequest.builder()
                .supplyLotId(lot.getId())
                .warehouseId(warehouse.getId())
                .locationId(location != null ? location.getId() : null)
                .movementType("IN")
                .quantity(request.getQuantity().abs())
                .note(request.getNote() != null ? request.getNote() : "Stock IN via Suppliers & Supplies")
                .build();
        StockMovementResponse movement = inventoryService.recordMovement(movementRequest);

        // 9. Return response
        return StockInResponse.builder()
                .supplyLot(toSupplyLotResponse(lot))
                .movement(movement)
                .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private void ensureWarehouseOwnership(Warehouse warehouse) {
        if (warehouse.getFarm() == null) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        farmAccessService.assertCurrentUserCanAccessFarm(warehouse.getFarm());
    }

    private SupplierResponse toSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .licenseNo(supplier.getLicenseNo())
                .contactEmail(supplier.getContactEmail())
                .contactPhone(supplier.getContactPhone())
                .build();
    }

    private SupplyItemResponse toSupplyItemResponse(SupplyItem item) {
        return SupplyItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .activeIngredient(item.getActiveIngredient())
                .unit(item.getUnit())
                .restrictedFlag(Boolean.TRUE.equals(item.getRestrictedFlag()))
                .build();
    }

    private SupplyLotResponse toSupplyLotResponse(SupplyLot lot) {
        return SupplyLotResponse.builder()
                .id(lot.getId())
                .batchCode(lot.getBatchCode())
                .expiryDate(lot.getExpiryDate())
                .status(lot.getStatus())
                .supplierId(lot.getSupplier() != null ? lot.getSupplier().getId() : null)
                .supplierName(lot.getSupplier() != null ? lot.getSupplier().getName() : null)
                .supplyItemId(lot.getSupplyItem() != null ? lot.getSupplyItem().getId() : null)
                .supplyItemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                .restrictedFlag(
                        lot.getSupplyItem() != null && Boolean.TRUE.equals(lot.getSupplyItem().getRestrictedFlag()))
                .build();
    }

    private List<InventoryBalance> loadPositiveBalancesForFarmIds(List<Integer> farmIds) {
        if (farmIds == null || farmIds.isEmpty()) {
            return List.of();
        }
        return inventoryBalanceRepository.findPositiveByFarmIdsWithDetails(farmIds);
    }

    private <T> PageResponse<T> toPageResponse(List<T> items, Pageable pageable) {
        int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : Math.max(items.size(), 1);
        int fromIndex = Math.min(pageNumber * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        List<T> pageItems = items.subList(fromIndex, toIndex);
        Page<T> page = new PageImpl<>(pageItems, pageable, items.size());
        return PageResponse.of(page, pageItems);
    }

    private String normalizeSupplyItemName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

}
