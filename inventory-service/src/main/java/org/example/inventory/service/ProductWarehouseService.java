package org.example.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.entity.ProductWarehouseLot;
import org.example.inventory.entity.ProductWarehouseTransaction;
import org.example.inventory.entity.StockLocation;
import org.example.inventory.entity.Warehouse;
import org.example.inventory.enums.ProductWarehouseLotStatus;
import org.example.inventory.enums.ProductWarehouseTransactionType;
import org.example.inventory.event.DomainEventPublisher;
import org.example.inventory.event.StockAdjustedEvent;
import org.example.inventory.exception.AppException;
import org.example.inventory.exception.ErrorCode;
import org.example.inventory.repository.ProductWarehouseLotRepository;
import org.example.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.inventory.repository.StockLocationRepository;
import org.example.inventory.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductWarehouseService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final String OUTPUT_WAREHOUSE_TYPE = "OUTPUT";
    static final String DEFAULT_HARVEST_UNIT = "kg";

    ProductWarehouseLotRepository productWarehouseLotRepository;
    ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    ObjectMapper objectMapper;
    DomainEventPublisher domainEventPublisher;

    public ProductWarehouseLot receiveFromHarvestEvent(
            Integer harvestId,
            Integer seasonId,
            String seasonName,
            Integer plotId,
            Integer farmId,
            String cropName,
            String varietyName,
            LocalDate harvestDate,
            BigDecimal quantity,
            String unit,
            String grade,
            String note,
            Long actorUserId) {
        Optional<ProductWarehouseLot> existingLot = productWarehouseLotRepository.findByHarvestId(harvestId);
        if (existingLot.isPresent()) {
            return existingLot.get();
        }

        Warehouse warehouse = resolveDefaultProductWarehouse(farmId);
        StockLocation location = resolveDefaultLocation(warehouse);
        BigDecimal normalizedQuantity = normalizePositiveQuantity(quantity);

        String lotCode = generateHarvestLotCode(harvestId, cropName, harvestDate);
        ensureUniqueLotCode(lotCode);

        String productName = cropName != null && !cropName.isBlank() ? cropName : "Harvest Product";
        String productVariant = varietyName != null && !varietyName.isBlank() ? varietyName : null;
        String normalizedUnit = unit != null && !unit.isBlank() ? normalizeUnit(unit) : DEFAULT_HARVEST_UNIT;
        LocalDateTime receivedAt = LocalDateTime.now();

        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .lotCode(lotCode)
                .productId(null)
                .productName(productName)
                .productVariant(productVariant)
                .seasonId(seasonId)
                .farmId(farmId)
                .plotId(plotId)
                .harvestId(harvestId)
                .warehouseId(warehouse.getId())
                .locationId(location != null ? location.getId() : null)
                .harvestedAt(harvestDate)
                .receivedAt(receivedAt)
                .unit(normalizedUnit)
                .initialQuantity(normalizedQuantity)
                .onHandQuantity(normalizedQuantity)
                .grade(normalizeBlankToNull(grade))
                .qualityStatus(normalizeBlankToNull(grade))
                .traceabilityData(buildHarvestTraceabilityData(harvestId, seasonId, seasonName, farmId, plotId, harvestDate, actorUserId, receivedAt, normalizedQuantity, grade, note))
                .note(normalizeBlankToNull(note))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .createdBy(actorUserId)
                .build();

        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);

        createTransaction(
                saved,
                ProductWarehouseTransactionType.RECEIPT_FROM_HARVEST,
                normalizedQuantity,
                "HARVEST",
                String.valueOf(harvestId),
                "Received from harvest event",
                actorUserId);

        publishStockAdjusted(saved, BigDecimal.ZERO, normalizedQuantity, "Received from harvest event", actorUserId);
        return saved;
    }

    private Warehouse resolveDefaultProductWarehouse(Integer farmId) {
        List<Warehouse> warehouses = warehouseRepository.findAllByFarmId(farmId);
        if (warehouses.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE);
        }
        return warehouses.stream()
                .filter(w -> w.getType() != null && OUTPUT_WAREHOUSE_TYPE.equalsIgnoreCase(w.getType()))
                .findFirst()
                .orElse(warehouses.get(0));
    }

    private StockLocation resolveDefaultLocation(Warehouse warehouse) {
        List<StockLocation> locations = stockLocationRepository.findAllByWarehouseId(warehouse.getId());
        if (locations.isEmpty()) {
            return null;
        }
        return locations.get(0);
    }

    private String generateHarvestLotCode(Integer harvestId, String cropName, LocalDate harvestDate) {
        String prefix = cropName != null && !cropName.isBlank()
                ? cropName.replaceAll("[^A-Za-z0-9]", "").toUpperCase()
                : "PRODUCT";
        if (prefix.isBlank()) {
            prefix = "PRODUCT";
        }
        String datePart = harvestDate != null
                ? harvestDate.format(DateTimeFormatter.BASIC_ISO_DATE)
                : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "PH-" + prefix + "-" + datePart + "-" + harvestId;
    }

    private void ensureUniqueLotCode(String lotCode) {
        if (productWarehouseLotRepository.findByLotCode(lotCode).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_INVALID_QUANTITY);
        }
        return quantity;
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_INVALID_UNIT);
        }
        String normalized = unit.trim();
        if (normalized.length() > 30) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_INVALID_UNIT);
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

    private ProductWarehouseTransaction createTransaction(
            ProductWarehouseLot lot,
            ProductWarehouseTransactionType type,
            BigDecimal quantity,
            String referenceType,
            String referenceId,
            String note,
            Long actor) {

        ProductWarehouseTransaction transaction = ProductWarehouseTransaction.builder()
                .lotId(lot.getId())
                .transactionType(type)
                .quantity(quantity)
                .unit(lot.getUnit())
                .resultingOnHand(lot.getOnHandQuantity())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(normalizeBlankToNull(note))
                .createdBy(actor)
                .build();
        return productWarehouseTransactionRepository.save(transaction);
    }

    private String buildHarvestTraceabilityData(
            Integer harvestId,
            Integer seasonId,
            String seasonName,
            Integer farmId,
            Integer plotId,
            LocalDate harvestedAt,
            Long actorUserId,
            LocalDateTime receivedAt,
            BigDecimal initialQuantity,
            String grade,
            String note) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("source", "HARVEST_EVENT");
        trace.put("harvestId", harvestId);
        trace.put("seasonId", seasonId);
        trace.put("seasonName", seasonName);
        trace.put("farmId", farmId);
        trace.put("plotId", plotId);
        trace.put("harvestedAt", harvestedAt);
        trace.put("receivedAt", receivedAt);
        trace.put("recordedBy", actorUserId);
        trace.put("initialQuantity", initialQuantity);
        trace.put("grade", grade);
        trace.put("note", note);
        try {
            return objectMapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void publishStockAdjusted(ProductWarehouseLot lot, BigDecimal previousQuantity, BigDecimal newQuantity, String reason, Long actorUserId) {
        BigDecimal quantityChange = newQuantity.subtract(previousQuantity);
        domainEventPublisher.publish(StockAdjustedEvent.builder()
                .aggregateType("ProductWarehouseLot")
                .aggregateId(String.valueOf(lot.getId()))
                .productWarehouseLotId(lot.getId())
                .lotCode(lot.getLotCode())
                .farmId(lot.getFarmId())
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .quantityChange(quantityChange)
                .unit(lot.getUnit())
                .reason(reason)
                .actorUserId(actorUserId)
                .build());
    }
}
