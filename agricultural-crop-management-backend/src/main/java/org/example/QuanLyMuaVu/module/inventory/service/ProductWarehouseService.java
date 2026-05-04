package org.example.QuanLyMuaVu.module.inventory.service;

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
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.dto.request.AdjustProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.CreateProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.StockOutProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseLotResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseOverviewResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseTraceabilityResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseTransactionResponse;
import org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockLocationRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
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
    SeasonQueryPort seasonQueryPort;
    FarmQueryPort farmQueryPort;
    HarvestQueryPort harvestQueryPort;
    WarehouseRepository warehouseRepository;
    StockLocationRepository stockLocationRepository;
    FarmAccessPort farmAccessService;
    ObjectMapper objectMapper;

    public ProductWarehouseOverviewResponse getOverview() {
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (accessibleFarmIds.isEmpty()) {
            return ProductWarehouseOverviewResponse.builder()
                    .totalLots(0)
                    .inStockLots(0)
                    .depletedLots(0)
                    .totalOnHandQuantity(BigDecimal.ZERO)
                    .build();
        }

        long totalLots = productWarehouseLotRepository.countByFarm_IdIn(accessibleFarmIds);
        long inStockLots = productWarehouseLotRepository.countByFarmIdsAndOnHandPositive(accessibleFarmIds);
        BigDecimal totalOnHand = Optional.ofNullable(productWarehouseLotRepository.sumOnHandByFarmIds(accessibleFarmIds))
                .orElse(BigDecimal.ZERO);

        return ProductWarehouseOverviewResponse.builder()
                .totalLots(totalLots)
                .inStockLots(inStockLots)
                .depletedLots(Math.max(totalLots - inStockLots, 0))
                .totalOnHandQuantity(totalOnHand)
                .build();
    }

    public PageResponse<ProductWarehouseLotResponse> listLots(
            Integer warehouseId,
            Integer locationId,
            Integer seasonId,
            Integer farmId,
            Integer plotId,
            LocalDate harvestedFrom,
            LocalDate harvestedTo,
            String status,
            String q,
            int page,
            int size) {

        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size), Sort.by("receivedAt").descending());

        if (accessibleFarmIds.isEmpty()) {
            Page<ProductWarehouseLotResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            return PageResponse.of(emptyPage, List.of());
        }

        ProductWarehouseLotStatus parsedStatus = parseLotStatus(status);
        String normalizedQ = normalizeQuery(q);

        Page<ProductWarehouseLot> lotsPage = productWarehouseLotRepository.searchLots(
                accessibleFarmIds,
                warehouseId,
                locationId,
                seasonId,
                farmId,
                plotId,
                harvestedFrom,
                harvestedTo,
                parsedStatus,
                normalizedQ,
                pageable);

        List<ProductWarehouseLotResponse> items = lotsPage.getContent().stream()
                .map(this::toLotResponse)
                .toList();

        return PageResponse.of(lotsPage, items);
    }

    public ProductWarehouseLotResponse getLotDetail(Integer id) {
        ProductWarehouseLot lot = getLotForCurrentFarmer(id);
        return toLotResponse(lot);
    }

    public ProductWarehouseLotResponse createLot(CreateProductWarehouseLotRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();

        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = farmQueryPort.findFarmById(request.getFarmId())
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessFarm(farm);

        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = farmQueryPort.findPlotById(request.getPlotId())
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
        ensurePlotBelongsToFarm(plot, farm);

        org.example.QuanLyMuaVu.module.season.entity.Season season = null;
        if (request.getSeasonId() != null) {
            season = seasonQueryPort.findSeasonById(request.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(season);
            ensureSeasonBelongsToFarm(season, farm);
        }

        org.example.QuanLyMuaVu.module.season.entity.Harvest harvest = null;
        if (request.getHarvestId() != null) {
            harvest = harvestQueryPort.findHarvestById(request.getHarvestId())
                    .orElseThrow(() -> new AppException(ErrorCode.HARVEST_NOT_FOUND));
            if (harvest.getSeason() == null) {
                throw new AppException(ErrorCode.SEASON_NOT_FOUND);
            }
            farmAccessService.assertCurrentUserCanAccessSeason(harvest.getSeason());
            ensureSeasonBelongsToFarm(harvest.getSeason(), farm);
            if (productWarehouseLotRepository.existsByHarvest_Id(request.getHarvestId())) {
                throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE);
            }
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessWarehouse(warehouse);
        ensureWarehouseBelongsToFarm(warehouse, farm);

        StockLocation location = resolveLocation(request.getLocationId(), warehouse);
        String lotCode = resolveLotCode(request.getLotCode(), request.getHarvestId());
        ensureUniqueLotCode(lotCode);

        BigDecimal initialQuantity = normalizePositiveQuantity(request.getInitialQuantity());

        ProductWarehouseLotStatus status = parseLotStatusOrDefault(request.getStatus(), ProductWarehouseLotStatus.IN_STOCK);
        String normalizedUnit = normalizeUnit(request.getUnit());
        LocalDateTime receivedAt = request.getReceivedAt() != null ? request.getReceivedAt() : LocalDateTime.now();

        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .lotCode(lotCode)
                .productId(request.getProductId())
                .productName(request.getProductName().trim())
                .productVariant(normalizeBlankToNull(request.getProductVariant()))
                .season(season)
                .farm(farm)
                .plot(plot)
                .harvest(harvest)
                .warehouse(warehouse)
                .location(location)
                .harvestedAt(request.getHarvestedAt())
                .receivedAt(receivedAt)
                .unit(normalizedUnit)
                .initialQuantity(initialQuantity)
                .onHandQuantity(initialQuantity)
                .grade(normalizeBlankToNull(request.getGrade()))
                .qualityStatus(normalizeBlankToNull(request.getQualityStatus()))
                .traceabilityData(resolveTraceabilityData(request.getTraceabilityData(), request, currentUser))
                .note(normalizeBlankToNull(request.getNote()))
                .status(status)
                .createdBy(currentUser)
                .build();

        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);

        ProductWarehouseTransactionType transactionType = harvest != null
                ? ProductWarehouseTransactionType.RECEIPT_FROM_HARVEST
                : ProductWarehouseTransactionType.RETURN;
        String referenceType = harvest != null ? "HARVEST" : "MANUAL_CREATE";
        String referenceId = harvest != null ? String.valueOf(harvest.getId()) : String.valueOf(saved.getId());

        createTransaction(
                saved,
                transactionType,
                initialQuantity,
                referenceType,
                referenceId,
                request.getNote(),
                currentUser);

        return toLotResponse(saved);
    }

    public ProductWarehouseLotResponse updateLot(Integer id, UpdateProductWarehouseLotRequest request) {
        ProductWarehouseLot lot = getLotForCurrentFarmerForUpdate(id);

        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            lot.setProductName(request.getProductName().trim());
        }
        if (request.getProductVariant() != null) {
            lot.setProductVariant(normalizeBlankToNull(request.getProductVariant()));
        }
        if (request.getLocationId() != null) {
            StockLocation location = resolveLocation(request.getLocationId(), lot.getWarehouse());
            lot.setLocation(location);
        }
        if (request.getGrade() != null) {
            lot.setGrade(normalizeBlankToNull(request.getGrade()));
        }
        if (request.getQualityStatus() != null) {
            lot.setQualityStatus(normalizeBlankToNull(request.getQualityStatus()));
        }
        if (request.getTraceabilityData() != null) {
            lot.setTraceabilityData(normalizeBlankToNull(request.getTraceabilityData()));
        }
        if (request.getNote() != null) {
            lot.setNote(normalizeBlankToNull(request.getNote()));
        }
        if (request.getStatus() != null) {
            lot.setStatus(parseLotStatusOrDefault(request.getStatus(), lot.getStatus()));
        }

        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);
        return toLotResponse(saved);
    }

    public void archiveLot(Integer id) {
        ProductWarehouseLot lot = getLotForCurrentFarmerForUpdate(id);
        lot.setStatus(ProductWarehouseLotStatus.ARCHIVED);
        productWarehouseLotRepository.save(lot);
    }

    public ProductWarehouseLotResponse adjustLot(Integer id, AdjustProductWarehouseLotRequest request) {
        ProductWarehouseLot lot = getLotForCurrentFarmerForUpdate(id);
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();

        BigDecimal delta = request.getQuantityDelta();
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_INVALID_QUANTITY);
        }
        if (request.getNote() == null || request.getNote().isBlank()) {
            throw new AppException(ErrorCode.ADJUST_NOTE_REQUIRED);
        }

        BigDecimal newOnHand = lot.getOnHandQuantity().add(delta);
        if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        lot.setOnHandQuantity(newOnHand);
        applyAutoStatus(lot);
        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);

        createTransaction(
                saved,
                ProductWarehouseTransactionType.ADJUSTMENT,
                delta,
                "LOT",
                String.valueOf(saved.getId()),
                request.getNote(),
                currentUser);

        return toLotResponse(saved);
    }

    public ProductWarehouseLotResponse stockOutLot(Integer id, StockOutProductWarehouseLotRequest request) {
        ProductWarehouseLot lot = getLotForCurrentFarmerForUpdate(id);
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();

        BigDecimal quantity = normalizePositiveQuantity(request.getQuantity());
        if (lot.getOnHandQuantity().compareTo(quantity) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        lot.setOnHandQuantity(lot.getOnHandQuantity().subtract(quantity));
        applyAutoStatus(lot);
        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);

        createTransaction(
                saved,
                ProductWarehouseTransactionType.STOCK_OUT,
                quantity,
                "LOT",
                String.valueOf(saved.getId()),
                request.getNote(),
                currentUser);

        return toLotResponse(saved);
    }

    public PageResponse<ProductWarehouseTransactionResponse> listTransactions(
            Integer lotId,
            String type,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size), Sort.by("createdAt").descending());

        if (accessibleFarmIds.isEmpty()) {
            Page<ProductWarehouseTransactionResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            return PageResponse.of(emptyPage, List.of());
        }

        if (lotId != null) {
            ProductWarehouseLot lot = getLotForCurrentFarmer(lotId);
            lotId = lot.getId();
        }

        ProductWarehouseTransactionType parsedType = parseTransactionType(type);
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : null;

        Page<ProductWarehouseTransaction> transactionPage = productWarehouseTransactionRepository.searchTransactions(
                accessibleFarmIds,
                lotId,
                parsedType,
                fromDate,
                toDate,
                pageable);

        List<ProductWarehouseTransactionResponse> items = transactionPage.getContent().stream()
                .map(this::toTransactionResponse)
                .toList();

        return PageResponse.of(transactionPage, items);
    }

    public ProductWarehouseTraceabilityResponse getTraceability(Integer lotId) {
        ProductWarehouseLot lot = getLotForCurrentFarmer(lotId);
        List<ProductWarehouseTransactionResponse> transactions = productWarehouseTransactionRepository
                .findAllByLot_IdOrderByCreatedAtDesc(lot.getId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();

        return ProductWarehouseTraceabilityResponse.builder()
                .lotId(lot.getId())
                .lotCode(lot.getLotCode())
                .productName(lot.getProductName())
                .productVariant(lot.getProductVariant())
                .seasonId(lot.getSeason() != null ? lot.getSeason().getId() : null)
                .seasonName(lot.getSeason() != null ? lot.getSeason().getSeasonName() : null)
                .farmId(lot.getFarm() != null ? lot.getFarm().getId() : null)
                .farmName(lot.getFarm() != null ? lot.getFarm().getName() : null)
                .plotId(lot.getPlot() != null ? lot.getPlot().getId() : null)
                .plotName(lot.getPlot() != null ? lot.getPlot().getPlotName() : null)
                .harvestId(lot.getHarvest() != null ? lot.getHarvest().getId() : null)
                .harvestedAt(lot.getHarvestedAt())
                .receivedAt(lot.getReceivedAt())
                .initialQuantity(lot.getInitialQuantity())
                .onHandQuantity(lot.getOnHandQuantity())
                .unit(lot.getUnit())
                .recordedBy(lot.getCreatedBy() != null ? lot.getCreatedBy().getId() : null)
                .recordedByName(resolveUserDisplayName(lot.getCreatedBy()))
                .traceabilityData(lot.getTraceabilityData())
                .transactions(transactions)
                .build();
    }

    public ProductWarehouseLot receiveFromHarvest(org.example.QuanLyMuaVu.module.season.entity.Harvest harvest, org.example.QuanLyMuaVu.module.identity.entity.User actor) {
        return receiveFromHarvest(harvest, actor, null);
    }

    public ProductWarehouseLot receiveFromHarvest(
            org.example.QuanLyMuaVu.module.season.entity.Harvest harvest,
            org.example.QuanLyMuaVu.module.identity.entity.User actor,
            ReceiveHarvestRequest request) {
        if (harvest == null || harvest.getId() == null) {
            throw new AppException(ErrorCode.HARVEST_NOT_FOUND);
        }

        Optional<ProductWarehouseLot> existingLot = productWarehouseLotRepository.findByHarvest_Id(harvest.getId());
        if (existingLot.isPresent()) {
            return existingLot.get();
        }

        org.example.QuanLyMuaVu.module.season.entity.Season season = harvest.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessSeason(season);

        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = season.getPlot();
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }
        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = plot.getFarm();
        if (farm == null) {
            throw new AppException(ErrorCode.FARM_NOT_FOUND);
        }

        Warehouse warehouse = resolveWarehouseForHarvestReceipt(farm, request);
        StockLocation location = resolveLocationForHarvestReceipt(warehouse, request);
        BigDecimal quantity = normalizePositiveQuantity(harvest.getQuantity());

        String lotCode = resolveLotCodeForHarvest(request, harvest, season);
        ensureUniqueLotCode(lotCode);

        String productName = resolveProductNameForHarvestReceipt(season, request);
        String productVariant = resolveProductVariantForHarvestReceipt(season, request);
        String unit = resolveHarvestUnit(request);
        Integer productId = request != null ? request.getProductId() : null;
        String note = request != null && request.getNote() != null ? request.getNote() : harvest.getNote();

        org.example.QuanLyMuaVu.module.identity.entity.User createdBy = actor != null ? actor : farmAccessService.getCurrentUser();
        LocalDateTime receivedAt = LocalDateTime.now();

        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .lotCode(lotCode)
                .productId(productId)
                .productName(productName)
                .productVariant(productVariant)
                .season(season)
                .farm(farm)
                .plot(plot)
                .harvest(harvest)
                .warehouse(warehouse)
                .location(location)
                .harvestedAt(harvest.getHarvestDate())
                .receivedAt(receivedAt)
                .unit(unit)
                .initialQuantity(quantity)
                .onHandQuantity(quantity)
                .grade(normalizeBlankToNull(harvest.getGrade()))
                .qualityStatus(normalizeBlankToNull(harvest.getGrade()))
                .traceabilityData(buildHarvestTraceabilityData(harvest, season, farm, plot, createdBy, receivedAt))
                .note(normalizeBlankToNull(note))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .createdBy(createdBy)
                .build();

        ProductWarehouseLot saved = productWarehouseLotRepository.save(lot);

        createTransaction(
                saved,
                ProductWarehouseTransactionType.RECEIPT_FROM_HARVEST,
                quantity,
                "HARVEST",
                String.valueOf(harvest.getId()),
                "Received from harvest",
                createdBy);

        return saved;
    }

    private ProductWarehouseLot getLotForCurrentFarmer(Integer lotId) {
        return getLotForCurrentFarmer(lotId, false);
    }

    private ProductWarehouseLot getLotForCurrentFarmerForUpdate(Integer lotId) {
        return getLotForCurrentFarmer(lotId, true);
    }

    private ProductWarehouseLot getLotForCurrentFarmer(Integer lotId, boolean forUpdate) {
        Optional<ProductWarehouseLot> lotOptional = forUpdate
                ? productWarehouseLotRepository.findByIdForUpdate(lotId)
                : productWarehouseLotRepository.findById(lotId);

        ProductWarehouseLot lot = lotOptional
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND));
        if (lot.getFarm() == null) {
            throw new AppException(ErrorCode.FARM_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessFarm(lot.getFarm());
        return lot;
    }

    private ProductWarehouseTransaction createTransaction(
            ProductWarehouseLot lot,
            ProductWarehouseTransactionType type,
            BigDecimal quantity,
            String referenceType,
            String referenceId,
            String note,
            org.example.QuanLyMuaVu.module.identity.entity.User actor) {

        ProductWarehouseTransaction transaction = ProductWarehouseTransaction.builder()
                .lot(lot)
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

    private Warehouse resolveDefaultProductWarehouse(org.example.QuanLyMuaVu.module.farm.entity.Farm farm) {
        List<Warehouse> warehouses = warehouseRepository.findAllByFarm(farm);
        if (warehouses.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_NO_OUTPUT_WAREHOUSE);
        }
        return warehouses.stream()
                .filter(w -> w.getType() != null && OUTPUT_WAREHOUSE_TYPE.equalsIgnoreCase(w.getType()))
                .findFirst()
                .orElse(warehouses.get(0));
    }

    private Warehouse resolveWarehouseForHarvestReceipt(
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm,
            ReceiveHarvestRequest request) {
        if (request == null || request.getWarehouseId() == null) {
            return resolveDefaultProductWarehouse(farm);
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessWarehouse(warehouse);
        ensureWarehouseBelongsToFarm(warehouse, farm);
        return warehouse;
    }

    private StockLocation resolveDefaultLocation(Warehouse warehouse) {
        List<StockLocation> locations = stockLocationRepository.findAllByWarehouse(warehouse);
        if (locations.isEmpty()) {
            return null;
        }
        return locations.get(0);
    }

    private StockLocation resolveLocationForHarvestReceipt(Warehouse warehouse, ReceiveHarvestRequest request) {
        if (request != null && request.getLocationId() != null) {
            return resolveLocation(request.getLocationId(), warehouse);
        }
        return resolveDefaultLocation(warehouse);
    }

    private StockLocation resolveLocation(Integer locationId, Warehouse warehouse) {
        if (locationId == null) {
            return null;
        }
        StockLocation location = stockLocationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        if (location.getWarehouse() == null || location.getWarehouse().getId() == null
                || !location.getWarehouse().getId().equals(warehouse.getId())) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOCATION_MISMATCH);
        }
        return location;
    }

    private void ensureWarehouseBelongsToFarm(Warehouse warehouse, org.example.QuanLyMuaVu.module.farm.entity.Farm farm) {
        if (warehouse.getFarm() == null || warehouse.getFarm().getId() == null || farm.getId() == null
                || !warehouse.getFarm().getId().equals(farm.getId())) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_FARM_MISMATCH);
        }
    }

    private void ensurePlotBelongsToFarm(org.example.QuanLyMuaVu.module.farm.entity.Plot plot, org.example.QuanLyMuaVu.module.farm.entity.Farm farm) {
        if (plot.getFarm() == null || plot.getFarm().getId() == null || farm.getId() == null
                || !plot.getFarm().getId().equals(farm.getId())) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_FARM_MISMATCH);
        }
    }

    private void ensureSeasonBelongsToFarm(org.example.QuanLyMuaVu.module.season.entity.Season season, org.example.QuanLyMuaVu.module.farm.entity.Farm farm) {
        if (season.getPlot() == null || season.getPlot().getFarm() == null || season.getPlot().getFarm().getId() == null
                || farm.getId() == null || !season.getPlot().getFarm().getId().equals(farm.getId())) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_FARM_MISMATCH);
        }
    }

    private String resolveLotCode(String requestedLotCode, Integer harvestId) {
        if (requestedLotCode != null && !requestedLotCode.isBlank()) {
            return requestedLotCode.trim();
        }
        if (harvestId != null) {
            return "PH-" + harvestId + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        return "PWL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String generateHarvestLotCode(org.example.QuanLyMuaVu.module.season.entity.Harvest harvest, org.example.QuanLyMuaVu.module.season.entity.Season season) {
        String prefix = season.getCrop() != null && season.getCrop().getCropName() != null
                ? season.getCrop().getCropName().replaceAll("[^A-Za-z0-9]", "").toUpperCase()
                : "PRODUCT";
        if (prefix.isBlank()) {
            prefix = "PRODUCT";
        }
        String datePart = harvest.getHarvestDate() != null
                ? harvest.getHarvestDate().format(DateTimeFormatter.BASIC_ISO_DATE)
                : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "PH-" + prefix + "-" + datePart + "-" + harvest.getId();
    }

    private String resolveLotCodeForHarvest(
            ReceiveHarvestRequest request,
            org.example.QuanLyMuaVu.module.season.entity.Harvest harvest,
            org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (request != null && request.getLotCode() != null && !request.getLotCode().isBlank()) {
            return request.getLotCode().trim();
        }
        return generateHarvestLotCode(harvest, season);
    }

    private void ensureUniqueLotCode(String lotCode) {
        if (productWarehouseLotRepository.findByLotCode(lotCode).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }

    private String resolveProductNameFromSeason(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season.getCrop() != null && season.getCrop().getCropName() != null && !season.getCrop().getCropName().isBlank()) {
            return season.getCrop().getCropName().trim();
        }
        return "org.example.QuanLyMuaVu.module.season.entity.Harvest Product";
    }

    private String resolveProductVariantFromSeason(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season.getVariety() != null && season.getVariety().getName() != null && !season.getVariety().getName().isBlank()) {
            return season.getVariety().getName().trim();
        }
        return null;
    }

    private String resolveProductNameForHarvestReceipt(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            ReceiveHarvestRequest request) {
        if (request != null && request.getProductName() != null && !request.getProductName().isBlank()) {
            return request.getProductName().trim();
        }
        return resolveProductNameFromSeason(season);
    }

    private String resolveProductVariantForHarvestReceipt(
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            ReceiveHarvestRequest request) {
        if (request != null && request.getProductVariant() != null && !request.getProductVariant().isBlank()) {
            return request.getProductVariant().trim();
        }
        return resolveProductVariantFromSeason(season);
    }

    private String resolveHarvestUnit(ReceiveHarvestRequest request) {
        if (request != null && request.getUnit() != null && !request.getUnit().isBlank()) {
            return normalizeUnit(request.getUnit());
        }
        return DEFAULT_HARVEST_UNIT;
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

    private String normalizeQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizePageSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, 100);
    }

    private ProductWarehouseLotStatus parseLotStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProductWarehouseLotStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private ProductWarehouseLotStatus parseLotStatusOrDefault(String status, ProductWarehouseLotStatus defaultStatus) {
        ProductWarehouseLotStatus parsed = parseLotStatus(status);
        return parsed != null ? parsed : defaultStatus;
    }

    private ProductWarehouseTransactionType parseTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return ProductWarehouseTransactionType.fromCode(type);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private void applyAutoStatus(ProductWarehouseLot lot) {
        if (lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED || lot.getStatus() == ProductWarehouseLotStatus.HOLD) {
            return;
        }
        if (lot.getOnHandQuantity() == null || lot.getOnHandQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            lot.setStatus(ProductWarehouseLotStatus.DEPLETED);
            return;
        }
        lot.setStatus(ProductWarehouseLotStatus.IN_STOCK);
    }

    private String buildHarvestTraceabilityData(
            org.example.QuanLyMuaVu.module.season.entity.Harvest harvest,
            org.example.QuanLyMuaVu.module.season.entity.Season season,
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm,
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            org.example.QuanLyMuaVu.module.identity.entity.User actor,
            LocalDateTime receivedAt) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("source", "HARVEST");
        trace.put("harvestId", harvest.getId());
        trace.put("seasonId", season.getId());
        trace.put("seasonName", season.getSeasonName());
        trace.put("farmId", farm.getId());
        trace.put("farmName", farm.getName());
        trace.put("plotId", plot.getId());
        trace.put("plotName", plot.getPlotName());
        trace.put("harvestedAt", harvest.getHarvestDate());
        trace.put("receivedAt", receivedAt);
        trace.put("recordedBy", actor != null ? actor.getId() : null);
        trace.put("recordedByName", resolveUserDisplayName(actor));
        trace.put("initialQuantity", harvest.getQuantity());
        trace.put("grade", harvest.getGrade());
        trace.put("note", harvest.getNote());
        try {
            return objectMapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String resolveTraceabilityData(
            String providedTraceabilityData,
            CreateProductWarehouseLotRequest request,
            org.example.QuanLyMuaVu.module.identity.entity.User actor) {
        if (providedTraceabilityData != null && !providedTraceabilityData.isBlank()) {
            return providedTraceabilityData;
        }
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("source", "MANUAL");
        trace.put("farmId", request.getFarmId());
        trace.put("plotId", request.getPlotId());
        trace.put("seasonId", request.getSeasonId());
        trace.put("harvestId", request.getHarvestId());
        trace.put("harvestedAt", request.getHarvestedAt());
        trace.put("receivedAt", request.getReceivedAt());
        trace.put("recordedBy", actor != null ? actor.getId() : null);
        trace.put("recordedByName", resolveUserDisplayName(actor));
        try {
            return objectMapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private ProductWarehouseLotResponse toLotResponse(ProductWarehouseLot lot) {
        return ProductWarehouseLotResponse.builder()
                .id(lot.getId())
                .lotCode(lot.getLotCode())
                .productId(lot.getProductId())
                .productName(lot.getProductName())
                .productVariant(lot.getProductVariant())
                .seasonId(lot.getSeason() != null ? lot.getSeason().getId() : null)
                .seasonName(lot.getSeason() != null ? lot.getSeason().getSeasonName() : null)
                .farmId(lot.getFarm() != null ? lot.getFarm().getId() : null)
                .farmName(lot.getFarm() != null ? lot.getFarm().getName() : null)
                .plotId(lot.getPlot() != null ? lot.getPlot().getId() : null)
                .plotName(lot.getPlot() != null ? lot.getPlot().getPlotName() : null)
                .harvestId(lot.getHarvest() != null ? lot.getHarvest().getId() : null)
                .warehouseId(lot.getWarehouse() != null ? lot.getWarehouse().getId() : null)
                .warehouseName(lot.getWarehouse() != null ? lot.getWarehouse().getName() : null)
                .locationId(lot.getLocation() != null ? lot.getLocation().getId() : null)
                .locationLabel(lot.getLocation() != null ? buildLocationLabel(lot.getLocation()) : null)
                .harvestedAt(lot.getHarvestedAt())
                .receivedAt(lot.getReceivedAt())
                .unit(lot.getUnit())
                .initialQuantity(lot.getInitialQuantity())
                .onHandQuantity(lot.getOnHandQuantity())
                .grade(lot.getGrade())
                .qualityStatus(lot.getQualityStatus())
                .traceabilityData(lot.getTraceabilityData())
                .note(lot.getNote())
                .status(lot.getStatus() != null ? lot.getStatus().name() : null)
                .createdBy(lot.getCreatedBy() != null ? lot.getCreatedBy().getId() : null)
                .createdByName(resolveUserDisplayName(lot.getCreatedBy()))
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .build();
    }

    private ProductWarehouseTransactionResponse toTransactionResponse(ProductWarehouseTransaction transaction) {
        return ProductWarehouseTransactionResponse.builder()
                .id(transaction.getId())
                .lotId(transaction.getLot() != null ? transaction.getLot().getId() : null)
                .lotCode(transaction.getLot() != null ? transaction.getLot().getLotCode() : null)
                .transactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .quantity(transaction.getQuantity())
                .unit(transaction.getUnit())
                .resultingOnHand(transaction.getResultingOnHand())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .note(transaction.getNote())
                .createdBy(transaction.getCreatedBy() != null ? transaction.getCreatedBy().getId() : null)
                .createdByName(resolveUserDisplayName(transaction.getCreatedBy()))
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String resolveUserDisplayName(org.example.QuanLyMuaVu.module.identity.entity.User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }

    private String buildLocationLabel(StockLocation location) {
        StringBuilder label = new StringBuilder();
        if (location.getZone() != null && !location.getZone().isBlank()) {
            label.append(location.getZone());
        }
        if (location.getAisle() != null && !location.getAisle().isBlank()) {
            if (label.length() > 0) {
                label.append("-");
            }
            label.append(location.getAisle());
        }
        if (location.getShelf() != null && !location.getShelf().isBlank()) {
            if (label.length() > 0) {
                label.append("-");
            }
            label.append(location.getShelf());
        }
        if (location.getBin() != null && !location.getBin().isBlank()) {
            if (label.length() > 0) {
                label.append("-");
            }
            label.append(location.getBin());
        }
        return label.length() > 0 ? label.toString() : "Location " + location.getId();
    }
}
