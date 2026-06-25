package org.example.inventory.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.dto.request.CreateSupplierRequest;
import org.example.inventory.dto.request.CreateSupplyItemRequest;
import org.example.inventory.dto.request.RecordStockMovementRequest;
import org.example.inventory.dto.request.StockInRequest;
import org.example.inventory.dto.request.UpdateSupplierRequest;
import org.example.inventory.dto.request.UpdateSupplyItemRequest;
import org.example.inventory.dto.response.StockInResponse;
import org.example.inventory.dto.response.SupplierResponse;
import org.example.inventory.dto.response.SupplyItemResponse;
import org.example.inventory.dto.response.SupplyLotResponse;
import org.example.inventory.entity.StockLocation;
import org.example.inventory.entity.Supplier;
import org.example.inventory.entity.SupplyItem;
import org.example.inventory.entity.SupplyLot;
import org.example.inventory.entity.Warehouse;
import org.example.inventory.exception.AppException;
import org.example.inventory.exception.ErrorCode;
import org.example.inventory.repository.InventoryBalanceRepository;
import org.example.inventory.repository.StockMovementRepository;
import org.example.inventory.repository.SupplierRepository;
import org.example.inventory.repository.SupplyItemRepository;
import org.example.inventory.repository.SupplyLotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SuppliesPublicService {

    SupplierRepository supplierRepository;
    SupplyItemRepository supplyItemRepository;
    SupplyLotRepository supplyLotRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    StockMovementRepository stockMovementRepository;
    InventoryPublicService inventoryPublicService;

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getSuppliers(String q, Pageable pageable) {
        Page<Supplier> page = supplierRepository.searchByName(normalizeQuery(q), pageable);
        List<SupplierResponse> items = page.getContent().stream()
                .map(this::toSupplierResponse)
                .toList();
        return PageResponse.of(page, items);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Integer id) {
        return toSupplierResponse(getSupplierEntity(id));
    }

    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(normalizeRequired(request.getName()))
                .licenseNo(normalizeBlankToNull(request.getLicenseNo()))
                .contactEmail(normalizeBlankToNull(request.getContactEmail()))
                .contactPhone(normalizeBlankToNull(request.getContactPhone()))
                .build();
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse updateSupplier(Integer id, UpdateSupplierRequest request) {
        Supplier supplier = getSupplierEntity(id);
        supplier.setName(normalizeRequired(request.getName()));
        supplier.setLicenseNo(normalizeBlankToNull(request.getLicenseNo()));
        supplier.setContactEmail(normalizeBlankToNull(request.getContactEmail()));
        supplier.setContactPhone(normalizeBlankToNull(request.getContactPhone()));
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    public void deleteSupplier(Integer id) {
        Supplier supplier = getSupplierEntity(id);
        if (!supplyLotRepository.findAllBySupplierId(id).isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        supplierRepository.delete(supplier);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplyItemResponse> getSupplyItems(String q, Boolean restricted, Pageable pageable) {
        Page<SupplyItem> page = supplyItemRepository.searchItems(normalizeQuery(q), restricted, pageable);
        List<SupplyItemResponse> items = page.getContent().stream()
                .map(this::toSupplyItemResponse)
                .toList();
        return PageResponse.of(page, items);
    }

    public SupplyItemResponse createSupplyItem(CreateSupplyItemRequest request) {
        String name = normalizeRequired(request.getName());
        if (supplyItemRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        SupplyItem item = SupplyItem.builder()
                .name(name)
                .activeIngredient(normalizeBlankToNull(request.getActiveIngredient()))
                .unit(normalizeRequired(request.getUnit()))
                .restrictedFlag(Boolean.TRUE.equals(request.getRestrictedFlag()))
                .build();
        return toSupplyItemResponse(supplyItemRepository.save(item));
    }

    public SupplyItemResponse updateSupplyItem(Integer id, UpdateSupplyItemRequest request) {
        SupplyItem item = getSupplyItem(id);
        String name = normalizeRequired(request.getName());
        if (!name.equalsIgnoreCase(item.getName()) && supplyItemRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        item.setName(name);
        item.setActiveIngredient(normalizeBlankToNull(request.getActiveIngredient()));
        item.setUnit(normalizeRequired(request.getUnit()));
        item.setRestrictedFlag(Boolean.TRUE.equals(request.getRestrictedFlag()));
        return toSupplyItemResponse(supplyItemRepository.save(item));
    }

    public void deleteSupplyItem(Integer id) {
        SupplyItem item = getSupplyItem(id);
        if (supplyLotRepository.existsBySupplyItemId(id)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        supplyItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplyLotResponse> getSupplyLots(
            Integer itemId,
            Integer supplierId,
            String status,
            String q,
            Pageable pageable) {
        Page<SupplyLot> page = supplyLotRepository.searchLots(
                itemId,
                supplierId,
                normalizeQuery(status),
                normalizeQuery(q),
                pageable);
        List<SupplyLotResponse> items = page.getContent().stream()
                .map(this::toSupplyLotResponse)
                .toList();
        return PageResponse.of(page, items);
    }

    public StockInResponse stockIn(StockInRequest request) {
        Warehouse warehouse = inventoryPublicService.getWarehouse(request.getWarehouseId());
        StockLocation location = null;
        if (request.getLocationId() != null) {
            location = inventoryPublicService.getLocation(request.getLocationId());
            inventoryPublicService.ensureLocationBelongsToWarehouse(location, warehouse.getId());
        }

        Supplier supplier = getSupplierEntity(request.getSupplierId());
        SupplyItem item = getSupplyItem(request.getSupplyItemId());
        if (Boolean.TRUE.equals(item.getRestrictedFlag()) && !Boolean.TRUE.equals(request.getConfirmRestricted())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        SupplyLot lot = SupplyLot.builder()
                .supplyItemId(item.getId())
                .supplierId(supplier.getId())
                .batchCode(normalizeBlankToNull(request.getBatchCode()))
                .expiryDate(parseDate(request.getExpiryDate()))
                .status("IN_STOCK")
                .build();
        lot = supplyLotRepository.save(lot);

        var movement = inventoryPublicService.recordMovement(RecordStockMovementRequest.builder()
                .supplyLotId(lot.getId())
                .warehouseId(warehouse.getId())
                .locationId(location != null ? location.getId() : null)
                .movementType("IN")
                .quantity(request.getQuantity())
                .note(request.getNote() != null ? request.getNote() : "Stock IN")
                .build());

        return StockInResponse.builder()
                .supplyLot(toSupplyLotResponse(lot))
                .movement(movement)
                .build();
    }

    private Supplier getSupplierEntity(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private SupplyItem getSupplyItem(Integer id) {
        return supplyItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
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
        Supplier supplier = lot.getSupplierId() != null
                ? supplierRepository.findById(lot.getSupplierId()).orElse(null)
                : null;
        SupplyItem item = lot.getSupplyItemId() != null
                ? supplyItemRepository.findById(lot.getSupplyItemId()).orElse(null)
                : null;

        return SupplyLotResponse.builder()
                .id(lot.getId())
                .batchCode(lot.getBatchCode())
                .expiryDate(lot.getExpiryDate())
                .status(lot.getStatus())
                .supplierId(lot.getSupplierId())
                .supplierName(supplier != null ? supplier.getName() : null)
                .supplyItemId(lot.getSupplyItemId())
                .supplyItemName(item != null ? item.getName() : null)
                .unit(item != null ? item.getUnit() : null)
                .restrictedFlag(item != null && Boolean.TRUE.equals(item.getRestrictedFlag()))
                .build();
    }

    private String normalizeQuery(String value) {
        return normalizeBlankToNull(value);
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseDate(String value) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        return LocalDate.parse(normalized);
    }
}
