package org.example.QuanLyMuaVu.module.season.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.HarvestChangedEvent;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.port.HarvestStockContextView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryCommandPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestStockContextResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestSummaryResponse;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.HarvestMapper;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
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
public class SeasonHarvestService {

    HarvestRepository harvestRepository;
    SeasonRepository seasonRepository;
    HarvestMapper harvestMapper;
    FarmAccessPort farmAccessService;
    InventoryCommandPort inventoryCommandPort;
    InventoryQueryPort inventoryQueryPort;
    ProductWarehouseLotRepository productWarehouseLotRepository;
    DomainEventPublisher domainEventPublisher;

    /**
     * List all harvests for the current farmer's seasons (supports "All Seasons"
     * filter).
     * If seasonId is provided, filters to that season only.
     */
    public PageResponse<HarvestResponse> listAllFarmerHarvests(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        List<Harvest> all;

        if (seasonId != null) {
            // Filter by specific season
            Season season = getSeasonForCurrentFarmer(seasonId);
            all = harvestRepository.findAllBySeason_Id(season.getId());
        } else {
            // List all harvests for farmer's seasons
            Long currentUserId = farmAccessService.getCurrentUserId();
            List<Season> farmerSeasons = seasonRepository.findAllByFarmUserId(currentUserId);
            List<Integer> seasonIds = farmerSeasons.stream()
                    .map(Season::getId)
                    .filter(id -> id != null)
                    .toList();

            if (seasonIds.isEmpty()) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
                Page<HarvestResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                return PageResponse.of(emptyPage, List.of());
            }

            all = harvestRepository.findAllBySeason_IdIn(seasonIds);
        }

        List<Harvest> filteredHarvests = all.stream()
                .filter(h -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = h.getHarvestDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .sorted((h1, h2) -> Integer.compare(
                        h2.getId() != null ? h2.getId() : 0,
                        h1.getId() != null ? h1.getId() : 0))
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filteredHarvests.size());
        List<Harvest> pageHarvests = fromIndex >= filteredHarvests.size() ? List.of() : filteredHarvests.subList(fromIndex, toIndex);
        List<HarvestResponse> pageItems = toResponsesWithStatuses(pageHarvests);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<HarvestResponse> pageData = new PageImpl<>(pageItems, pageable, filteredHarvests.size());

        return PageResponse.of(pageData, pageItems);
    }

    /**
     * Get harvest summary/KPI for a specific season or all farmer seasons.
     */
    public HarvestSummaryResponse getSummary(Integer seasonId) {
        BigDecimal totalHarvestedKg = BigDecimal.ZERO;
        int lotsCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal expectedYieldKg = null;
        BigDecimal actualYieldKg = null;
        BigDecimal yieldVsPlanPercent = null;
        List<Harvest> harvestsForSummary = List.of();
        List<Integer> summarySeasonIds = List.of();

        if (seasonId != null) {
            // Summary for specific season
            Season season = getSeasonForCurrentFarmer(seasonId);
            List<Harvest> harvests = harvestRepository.findAllBySeason_Id(season.getId());
            harvestsForSummary = harvests;
            summarySeasonIds = season.getId() != null ? List.of(season.getId()) : List.of();

            lotsCount = harvests.size();
            totalHarvestedKg = harvests.stream()
                    .map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalRevenue = harvests.stream()
                    .map(h -> {
                        BigDecimal qty = h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO;
                        BigDecimal unit = h.getUnit() != null ? h.getUnit() : BigDecimal.ZERO;
                        return qty.multiply(unit);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            expectedYieldKg = season.getExpectedYieldKg();
            actualYieldKg = season.getActualYieldKg() != null ? season.getActualYieldKg() : totalHarvestedKg;

            if (expectedYieldKg != null && expectedYieldKg.compareTo(BigDecimal.ZERO) > 0) {
                yieldVsPlanPercent = actualYieldKg.divide(expectedYieldKg, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        } else {
            // Summary for all farmer seasons
            Long currentUserId = farmAccessService.getCurrentUserId();
            List<Season> farmerSeasons = seasonRepository.findAllByFarmUserId(currentUserId);
            List<Integer> seasonIds = farmerSeasons.stream()
                    .map(Season::getId)
                    .filter(id -> id != null)
                    .toList();
            summarySeasonIds = seasonIds;

            if (!seasonIds.isEmpty()) {
                List<Harvest> allHarvests = harvestRepository.findAllBySeason_IdIn(seasonIds);
                harvestsForSummary = allHarvests;
                lotsCount = allHarvests.size();
                totalHarvestedKg = allHarvests.stream()
                        .map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalRevenue = allHarvests.stream()
                        .map(h -> {
                            BigDecimal qty = h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO;
                            BigDecimal unit = h.getUnit() != null ? h.getUnit() : BigDecimal.ZERO;
                            return qty.multiply(unit);
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Aggregate expected and actual yields from all seasons
                expectedYieldKg = farmerSeasons.stream()
                        .map(s -> s.getExpectedYieldKg() != null ? s.getExpectedYieldKg() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                actualYieldKg = farmerSeasons.stream()
                        .map(s -> s.getActualYieldKg() != null ? s.getActualYieldKg() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (expectedYieldKg.compareTo(BigDecimal.ZERO) > 0) {
                    yieldVsPlanPercent = actualYieldKg.divide(expectedYieldKg, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }
        }

        HarvestWarehouseSummary warehouseSummary = buildWarehouseSummary(summarySeasonIds);
        BigDecimal premiumGradePercentage = calculatePremiumGradePercentage(harvestsForSummary);

        return HarvestSummaryResponse.builder()
                .totalHarvestedKg(totalHarvestedKg)
                .lotsCount(lotsCount)
                .totalRevenue(totalRevenue)
                .yieldVsPlanPercent(yieldVsPlanPercent)
                .expectedYieldKg(expectedYieldKg)
                .actualYieldKg(actualYieldKg)
                .totalStoredKg(warehouseSummary.totalStoredKg())
                .totalSoldKg(warehouseSummary.totalSoldKg())
                .totalProcessingKg(warehouseSummary.totalProcessingKg())
                .premiumGradePercentage(premiumGradePercentage)
                .build();
    }

    private HarvestWarehouseSummary buildWarehouseSummary(List<Integer> seasonIds) {
        if (seasonIds == null || seasonIds.isEmpty()) {
            return new HarvestWarehouseSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalStoredKg = BigDecimal.ZERO;
        BigDecimal totalSoldKg = BigDecimal.ZERO;
        BigDecimal totalProcessingKg = BigDecimal.ZERO;

        List<ProductWarehouseLot> lots = productWarehouseLotRepository.findAllBySeason_IdIn(seasonIds);
        for (ProductWarehouseLot lot : lots) {
            if (lot == null || lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED) {
                continue;
            }

            BigDecimal onHand = safeQuantity(lot.getOnHandQuantity());
            BigDecimal initial = safeQuantity(lot.getInitialQuantity());
            BigDecimal movedOut = initial.subtract(onHand);
            if (movedOut.compareTo(BigDecimal.ZERO) > 0) {
                totalSoldKg = totalSoldKg.add(movedOut);
            }

            if (lot.getStatus() == ProductWarehouseLotStatus.HOLD) {
                totalProcessingKg = totalProcessingKg.add(onHand);
            } else if (lot.getStatus() == ProductWarehouseLotStatus.IN_STOCK) {
                totalStoredKg = totalStoredKg.add(onHand);
            }
        }

        return new HarvestWarehouseSummary(totalStoredKg, totalSoldKg, totalProcessingKg);
    }

    private BigDecimal calculatePremiumGradePercentage(List<Harvest> harvests) {
        if (harvests == null || harvests.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long premiumCount = harvests.stream()
                .filter(harvest -> harvest.getGrade() != null)
                .filter(harvest -> "PREMIUM".equalsIgnoreCase(harvest.getGrade().trim()))
                .count();

        return BigDecimal.valueOf(premiumCount)
                .divide(BigDecimal.valueOf(harvests.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal safeQuantity(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record HarvestWarehouseSummary(
            BigDecimal totalStoredKg,
            BigDecimal totalSoldKg,
            BigDecimal totalProcessingKg) {
    }

    public PageResponse<HarvestResponse> listHarvestsForSeason(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);

        List<Harvest> all = harvestRepository.findAllBySeason_Id(season.getId());

        List<Harvest> filteredHarvests = all.stream()
                .filter(h -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = h.getHarvestDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .sorted((h1, h2) -> Integer.compare(
                        h2.getId() != null ? h2.getId() : 0,
                        h1.getId() != null ? h1.getId() : 0))
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filteredHarvests.size());
        List<Harvest> pageHarvests = fromIndex >= filteredHarvests.size() ? List.of() : filteredHarvests.subList(fromIndex, toIndex);
        List<HarvestResponse> pageItems = toResponsesWithStatuses(pageHarvests);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<HarvestResponse> pageData = new PageImpl<>(pageItems, pageable, filteredHarvests.size());

        return PageResponse.of(pageData, pageItems);
    }

    public HarvestResponse createHarvest(Integer seasonId, CreateHarvestDetailRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonAllowsHarvest(season);

        validateHarvestDateWithinSeason(season, request.getHarvestDate());

        Harvest harvest = Harvest.builder()
                .season(season)
                .harvestDate(request.getHarvestDate())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .grade(request.getGrade())
                .note(request.getNote())
                .build();

        Harvest saved = harvestRepository.save(harvest);
        ProductWarehouseLot receivedLot = inventoryCommandPort.receiveFromHarvest(
                saved.getId(),
                farmAccessService.getCurrentUserId(),
                ReceiveHarvestRequest.builder()
                        .warehouseId(request.getWarehouseId())
                        .locationId(request.getLocationId())
                        .productId(request.getProductId())
                        .productName(request.getProductName())
                        .productVariant(request.getProductVariant())
                        .lotCode(request.getLotCode())
                        .unit(request.getInventoryUnit())
                        .note(request.getNote())
                        .build());
        recomputeSeasonActualYield(season);
        domainEventPublisher.publish(new HarvestChangedEvent(saved, HarvestChangedEvent.Action.RECORDED));
        return toResponseWithStatus(saved, receivedLot);
    }

    @Transactional(readOnly = true)
    public HarvestStockContextResponse getStockContext(
            Integer seasonId,
            Integer warehouseId,
            String productName,
            String lotCode) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        if (season.getPlot() == null || season.getPlot().getFarm() == null || season.getPlot().getFarm().getId() == null) {
            throw new AppException(ErrorCode.FARM_NOT_FOUND);
        }
        Integer farmId = season.getPlot().getFarm().getId();
        HarvestStockContextView context = inventoryQueryPort.findHarvestStockContext(
                farmId,
                warehouseId,
                productName,
                lotCode)
                .orElse(null);

        return HarvestStockContextResponse.builder()
                .warehouseId(warehouseId)
                .warehouseName(context != null ? context.warehouseName() : null)
                .productName(productName)
                .lotCode(lotCode)
                .matchingLots(context != null ? context.matchingLots() : 0L)
                .onHandQuantity(context != null ? context.onHandQuantity() : BigDecimal.ZERO)
                .unit(context != null ? context.unit() : null)
                .build();
    }

    public HarvestResponse getHarvest(Integer id) {
        Harvest harvest = getHarvestForCurrentFarmer(id);
        return toResponseWithStatus(harvest);
    }

    public HarvestResponse updateHarvest(Integer id, UpdateHarvestDetailRequest request) {
        Harvest harvest = getHarvestForCurrentFarmer(id);
        ensureSeasonAllowsHarvest(harvest.getSeason());
        ProductWarehouseLot linkedLot = harvest.getId() != null
                ? productWarehouseLotRepository.findByHarvest_Id(harvest.getId()).orElse(null)
                : null;

        validateHarvestDateWithinSeason(harvest.getSeason(), request.getHarvestDate());
        validateLinkedLotQuantityUpdate(linkedLot, request.getQuantity());

        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setQuantity(request.getQuantity());
        harvest.setUnit(request.getUnit());
        harvest.setGrade(request.getGrade());
        harvest.setNote(request.getNote());

        Harvest saved = harvestRepository.save(harvest);
        ProductWarehouseLot savedLot = syncLinkedLotFromHarvest(linkedLot, saved);
        recomputeSeasonActualYield(harvest.getSeason());
        domainEventPublisher.publish(new HarvestChangedEvent(saved, HarvestChangedEvent.Action.UPDATED));
        return toResponseWithStatus(saved, savedLot);
    }

    private List<HarvestResponse> toResponsesWithStatuses(List<Harvest> harvests) {
        if (harvests == null || harvests.isEmpty()) {
            return List.of();
        }

        Map<Integer, ProductWarehouseLot> lotByHarvestId = loadLotByHarvestId(harvests);
        return harvests.stream()
                .map(harvest -> toResponseWithStatus(harvest, lotByHarvestId.get(harvest.getId())))
                .toList();
    }

    private Map<Integer, ProductWarehouseLot> loadLotByHarvestId(List<Harvest> harvests) {
        List<Integer> harvestIds = harvests.stream()
                .map(Harvest::getId)
                .filter(id -> id != null)
                .toList();
        if (harvestIds.isEmpty()) {
            return Map.of();
        }

        return productWarehouseLotRepository.findAllByHarvest_IdIn(harvestIds).stream()
                .filter(lot -> lot.getHarvest() != null && lot.getHarvest().getId() != null)
                .collect(Collectors.toMap(
                        lot -> lot.getHarvest().getId(),
                        lot -> lot,
                        (existing, replacement) -> existing));
    }

    private HarvestResponse toResponseWithStatus(Harvest harvest) {
        ProductWarehouseLot lot = harvest != null && harvest.getId() != null
                ? productWarehouseLotRepository.findByHarvest_Id(harvest.getId()).orElse(null)
                : null;
        return toResponseWithStatus(harvest, lot);
    }

    private HarvestResponse toResponseWithStatus(Harvest harvest, ProductWarehouseLot lot) {
        HarvestResponse response = harvestMapper.toResponse(harvest);
        if (response != null) {
            response.setStatus(resolveHarvestStatus(lot));
        }
        return response;
    }

    private String resolveHarvestStatus(ProductWarehouseLot lot) {
        if (lot == null || lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED) {
            return null;
        }
        if (lot.getStatus() == ProductWarehouseLotStatus.HOLD) {
            return "processing";
        }
        BigDecimal onHand = safeQuantity(lot.getOnHandQuantity());
        if (lot.getStatus() == ProductWarehouseLotStatus.DEPLETED || onHand.compareTo(BigDecimal.ZERO) <= 0) {
            return "sold";
        }
        if (lot.getStatus() == ProductWarehouseLotStatus.IN_STOCK) {
            return "stored";
        }
        return null;
    }

    private void validateLinkedLotQuantityUpdate(ProductWarehouseLot lot, BigDecimal nextQuantity) {
        if (lot == null || lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED) {
            return;
        }
        BigDecimal movedOut = calculateMovedOutQuantity(lot);
        if (nextQuantity.compareTo(movedOut) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private ProductWarehouseLot syncLinkedLotFromHarvest(ProductWarehouseLot lot, Harvest harvest) {
        if (lot == null || lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED) {
            return lot;
        }

        BigDecimal movedOut = calculateMovedOutQuantity(lot);
        BigDecimal nextOnHand = harvest.getQuantity().subtract(movedOut);

        lot.setHarvestedAt(harvest.getHarvestDate());
        lot.setInitialQuantity(harvest.getQuantity());
        lot.setOnHandQuantity(nextOnHand);
        lot.setGrade(normalizeBlankToNull(harvest.getGrade()));
        lot.setQualityStatus(normalizeBlankToNull(harvest.getGrade()));
        lot.setNote(normalizeBlankToNull(harvest.getNote()));
        applyAutoLotStatus(lot);
        return productWarehouseLotRepository.save(lot);
    }

    private BigDecimal calculateMovedOutQuantity(ProductWarehouseLot lot) {
        BigDecimal initial = safeQuantity(lot.getInitialQuantity());
        BigDecimal onHand = safeQuantity(lot.getOnHandQuantity());
        BigDecimal movedOut = initial.subtract(onHand);
        return movedOut.compareTo(BigDecimal.ZERO) > 0 ? movedOut : BigDecimal.ZERO;
    }

    private void applyAutoLotStatus(ProductWarehouseLot lot) {
        if (lot.getStatus() == ProductWarehouseLotStatus.ARCHIVED || lot.getStatus() == ProductWarehouseLotStatus.HOLD) {
            return;
        }
        if (safeQuantity(lot.getOnHandQuantity()).compareTo(BigDecimal.ZERO) <= 0) {
            lot.setStatus(ProductWarehouseLotStatus.DEPLETED);
            return;
        }
        lot.setStatus(ProductWarehouseLotStatus.IN_STOCK);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void deleteHarvest(Integer id) {
        Harvest harvest = getHarvestForCurrentFarmer(id);
        ensureSeasonAllowsHarvest(harvest.getSeason());
        ensureHarvestNotReceivedToProductWarehouse(harvest);

        Season season = harvest.getSeason();
        harvestRepository.delete(harvest);
        if (season != null) {
            recomputeSeasonActualYield(season);
        }
        domainEventPublisher.publish(new HarvestChangedEvent(harvest, HarvestChangedEvent.Action.DELETED));
    }

    private void ensureSeasonAllowsHarvest(Season season) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }
    }

    private void validateHarvestDateWithinSeason(Season season, LocalDate date) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();

        if (start == null || date.isBefore(start)) {
            throw new AppException(ErrorCode.HARVEST_DATE_BEFORE_PLANTING);
        }
        if (end != null && date.isAfter(end)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    private Harvest getHarvestForCurrentFarmer(Integer id) {
        Harvest harvest = harvestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.HARVEST_NOT_FOUND));

        Season season = harvest.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }

        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return harvest;
    }

    private Season getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private void recomputeSeasonActualYield(Season season) {
        if (season == null || season.getId() == null) {
            return;
        }
        List<Harvest> harvests = harvestRepository.findAllBySeason_Id(season.getId());
        BigDecimal total = harvests.stream()
                .map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        season.setActualYieldKg(total);
        seasonRepository.save(season);
    }

    private void ensureHarvestNotReceivedToProductWarehouse(Harvest harvest) {
        if (harvest == null || harvest.getId() == null) {
            return;
        }
        if (inventoryQueryPort.existsProductWarehouseLotByHarvestId(harvest.getId())) {
            throw new AppException(ErrorCode.HARVEST_ALREADY_RECEIVED_TO_PRODUCT_WAREHOUSE);
        }
    }
}
