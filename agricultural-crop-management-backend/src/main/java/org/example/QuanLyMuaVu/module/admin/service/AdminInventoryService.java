package org.example.QuanLyMuaVu.module.admin.service;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryHealthResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryLotDetailResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryMovementResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryOptionsResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryRiskLotResponse;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AdminInventoryService {

    InventoryQueryPort inventoryQueryPort;
    FarmQueryPort farmQueryPort;

    public AdminInventoryOptionsResponse getOptions() {
        List<AdminInventoryOptionsResponse.FarmOption> farms = farmQueryPort.findAllFarms()
                .stream()
                .sorted(Comparator.comparing(org.example.QuanLyMuaVu.module.farm.entity.Farm::getName, String.CASE_INSENSITIVE_ORDER))
                .map(farm -> AdminInventoryOptionsResponse.FarmOption.builder()
                        .id(farm.getId())
                        .name(farm.getName())
                        .build())
                .toList();

        List<String> categories = inventoryQueryPort.findAllSupplyLots()
                .stream()
                .map(org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot::getStatus)
                .filter(status -> status != null && !status.isBlank())
                .map(status -> status.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();

        List<AdminInventoryOptionsResponse.ItemOption> items = inventoryQueryPort.findAllSupplyLots()
                .stream()
                .filter(lot -> lot.getSupplyItem() != null && lot.getSupplyItem().getId() != null)
                .map(lot -> AdminInventoryOptionsResponse.ItemOption.builder()
                        .id(lot.getSupplyItem().getId())
                        .name(lot.getSupplyItem().getName() != null ? lot.getSupplyItem().getName() : "Unknown Item")
                        .build())
                .collect(Collectors.toMap(
                        AdminInventoryOptionsResponse.ItemOption::getId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(AdminInventoryOptionsResponse.ItemOption::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return AdminInventoryOptionsResponse.builder()
                .farms(farms)
                .categories(categories)
                .items(items)
                .build();
    }

    public PageResponse<AdminInventoryRiskLotResponse> listRiskLots(
            Integer farmId,
            Integer itemId,
            String status,
            String severity,
            Integer windowDays,
            String q,
            String sort,
            BigDecimal lowStockThreshold,
            Integer page,
            Integer limit) {

        int safeWindowDays = normalizeWindowDays(windowDays);
        int safePage = Math.max(page != null ? page : 0, 0);
        int safeLimit = normalizeLimit(limit);
        BigDecimal threshold = lowStockThreshold != null ? lowStockThreshold : BigDecimal.valueOf(5);
        String statusFilter = status != null ? status.trim().toUpperCase(Locale.ROOT) : "RISK";
        String severityFilter = severity != null ? severity.trim().toUpperCase(Locale.ROOT) : "ALL";
        String searchKeyword = q != null ? q.trim().toLowerCase(Locale.ROOT) : null;

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate cutoff = today.plusDays(safeWindowDays);
        Map<Integer, Boolean> abnormalByLotId = new LinkedHashMap<>();

        List<AdminInventoryRiskLotResponse> filtered = new ArrayList<>();
        for (LotAggregate aggregate : buildLotAggregates()) {
            if (aggregate.onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            org.example.QuanLyMuaVu.module.farm.entity.Farm selectedFarm = resolveFarmForResult(aggregate, farmId);
            if (farmId != null && selectedFarm == null) {
                continue;
            }

            org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot lot = aggregate.lot;
            if (itemId != null && (lot.getSupplyItem() == null || !itemId.equals(lot.getSupplyItem().getId()))) {
                continue;
            }
            if (searchKeyword != null && !searchKeyword.isBlank()) {
                String itemName = lot.getSupplyItem() != null && lot.getSupplyItem().getName() != null
                        ? lot.getSupplyItem().getName().toLowerCase(Locale.ROOT)
                        : "";
                String batchCode = lot.getBatchCode() != null ? lot.getBatchCode().toLowerCase(Locale.ROOT) : "";
                if (!itemName.contains(searchKeyword) && !batchCode.contains(searchKeyword)) {
                    continue;
                }
            }

            boolean abnormalMovement = abnormalByLotId.computeIfAbsent(
                    lot.getId(),
                    ignored -> hasAbnormalMovement(lot.getId(), aggregate.onHand, safeWindowDays, now));

            String riskStatus = resolveRiskStatus(
                    lot.getExpiryDate(),
                    today,
                    cutoff,
                    aggregate.onHand,
                    threshold,
                    abnormalMovement);
            if (!matchesStatusFilter(statusFilter, riskStatus)) {
                continue;
            }
            String riskSeverity = resolveSeverity(riskStatus, lot.getExpiryDate(), today);
            if (!matchesSeverityFilter(severityFilter, riskSeverity)) {
                continue;
            }

            Long daysToExpiry = lot.getExpiryDate() != null
                    ? ChronoUnit.DAYS.between(today, lot.getExpiryDate())
                    : null;

            AdminInventoryRiskLotResponse response = AdminInventoryRiskLotResponse.builder()
                    .lotId(lot.getId())
                    .itemId(lot.getSupplyItem() != null ? lot.getSupplyItem().getId() : null)
                    .itemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : "Unknown Item")
                    .lotCode(lot.getBatchCode())
                    .farmId(selectedFarm != null ? selectedFarm.getId() : null)
                    .farmName(selectedFarm != null ? selectedFarm.getName() : null)
                    .expiryDate(lot.getExpiryDate() != null ? lot.getExpiryDate().toString() : null)
                    .onHand(toDouble(aggregate.onHand))
                    .daysToExpiry(daysToExpiry)
                    .status(riskStatus)
                    .severity(riskSeverity)
                    .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                    .unitCost(null)
                    .build();

            filtered.add(response);
        }

        filtered.sort(buildSortComparator(sort));

        int totalElements = filtered.size();
        int fromIndex = safePage * safeLimit;
        int toIndex = Math.min(fromIndex + safeLimit, totalElements);
        List<AdminInventoryRiskLotResponse> pagedItems = fromIndex < totalElements
                ? filtered.subList(fromIndex, toIndex)
                : List.of();

        PageResponse<AdminInventoryRiskLotResponse> pageResponse = new PageResponse<>();
        pageResponse.setItems(pagedItems);
        pageResponse.setPage(safePage);
        pageResponse.setSize(safeLimit);
        pageResponse.setTotalElements(totalElements);
        pageResponse.setTotalPages(totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeLimit));
        return pageResponse;
    }

    public AdminInventoryLotDetailResponse getLotDetail(Integer lotId) {
        org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot lot = inventoryQueryPort.findSupplyLotByIdWithDetails(lotId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));

        List<org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance> balances = inventoryQueryPort.findInventoryBalancesBySupplyLotId(lotId);
        List<AdminInventoryLotDetailResponse.BalanceRow> balanceRows = balances.stream()
                .map(balance -> AdminInventoryLotDetailResponse.BalanceRow.builder()
                        .warehouseId(balance.getWarehouse() != null ? balance.getWarehouse().getId() : null)
                        .warehouseName(balance.getWarehouse() != null ? balance.getWarehouse().getName() : null)
                        .farmId(balance.getWarehouse() != null && balance.getWarehouse().getFarm() != null
                                ? balance.getWarehouse().getFarm().getId()
                                : null)
                        .farmName(balance.getWarehouse() != null && balance.getWarehouse().getFarm() != null
                                ? balance.getWarehouse().getFarm().getName()
                                : null)
                        .locationId(balance.getLocation() != null ? balance.getLocation().getId() : null)
                        .locationLabel(balance.getLocation() != null ? buildLocationLabel(balance.getLocation()) : null)
                        .quantity(toDouble(balance.getQuantity()))
                        .build())
                .toList();

        BigDecimal onHandTotal = balances.stream()
                .map(org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance::getQuantity)
                .filter(quantity -> quantity != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminInventoryLotDetailResponse.builder()
                .lotId(lot.getId())
                .itemId(lot.getSupplyItem() != null ? lot.getSupplyItem().getId() : null)
                .itemName(lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : null)
                .lotCode(lot.getBatchCode())
                .unit(lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null)
                .supplierName(lot.getSupplier() != null ? lot.getSupplier().getName() : null)
                .expiryDate(lot.getExpiryDate() != null ? lot.getExpiryDate().toString() : null)
                .status(lot.getStatus())
                .onHandTotal(toDouble(onHandTotal))
                .balances(balanceRows)
                .build();
    }

    public List<AdminInventoryMovementResponse> getLotMovements(Integer lotId) {
        if (!inventoryQueryPort.existsSupplyLotById(lotId)) {
            throw new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND);
        }

        return inventoryQueryPort.findStockMovementsBySupplyLotId(lotId)
                .stream()
                .map(this::toMovementResponse)
                .toList();
    }

    public AdminInventoryHealthResponse getInventoryHealth(Integer windowDays, Boolean includeExpiring, Integer limit) {
        int safeWindowDays = normalizeWindowDays(windowDays);
        boolean includeExpiringValue = includeExpiring == null || includeExpiring;
        int safeLimit = limit == null || limit <= 0 ? 5 : Math.min(limit, 50);

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(safeWindowDays);

        int totalExpiredLots = 0;
        int totalExpiringLots = 0;
        int totalUnknownExpiryLots = 0;
        BigDecimal totalQtyAtRisk = BigDecimal.ZERO;

        Map<Integer, FarmHealthAccumulator> farmAccumulatorMap = new LinkedHashMap<>();
        for (LotAggregate aggregate : buildLotAggregates()) {
            if (aggregate.onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            for (org.example.QuanLyMuaVu.module.farm.entity.Farm farm : aggregate.farmsById.values()) {
                if (farm == null) {
                    continue;
                }

                String lotStatus = resolveHealthStatus(aggregate.lot.getExpiryDate(), today, cutoff, includeExpiringValue);
                if ("HEALTHY".equals(lotStatus)) {
                    continue;
                }

                FarmHealthAccumulator farmAccumulator = farmAccumulatorMap.computeIfAbsent(
                        farm.getId(),
                        ignored -> new FarmHealthAccumulator(farm.getId(), farm.getName()));

                if ("EXPIRED".equals(lotStatus)) {
                    totalExpiredLots++;
                    farmAccumulator.expiredLots++;
                } else if ("EXPIRING".equals(lotStatus)) {
                    totalExpiringLots++;
                    farmAccumulator.expiringLots++;
                } else if ("UNKNOWN_EXPIRY".equals(lotStatus)) {
                    totalUnknownExpiryLots++;
                }

                totalQtyAtRisk = totalQtyAtRisk.add(aggregate.onHand);
                farmAccumulator.qtyAtRisk = farmAccumulator.qtyAtRisk.add(aggregate.onHand);

                farmAccumulator.topRiskLots.add(AdminInventoryHealthResponse.RiskLot.builder()
                        .lotId(aggregate.lot.getId())
                        .itemName(aggregate.lot.getSupplyItem() != null ? aggregate.lot.getSupplyItem().getName() : "Unknown Item")
                        .expiryDate(aggregate.lot.getExpiryDate() != null ? aggregate.lot.getExpiryDate().toString() : null)
                        .onHand(toDouble(aggregate.onHand))
                        .status(lotStatus)
                        .build());
            }
        }

        List<AdminInventoryHealthResponse.FarmHealth> farms = farmAccumulatorMap.values()
                .stream()
                .sorted(Comparator
                        .comparingInt(FarmHealthAccumulator::getExpiredLots).reversed()
                        .thenComparingInt(FarmHealthAccumulator::getExpiringLots).reversed()
                        .thenComparing((FarmHealthAccumulator acc) -> acc.qtyAtRisk, Comparator.reverseOrder()))
                .limit(safeLimit)
                .map(this::toFarmHealthResponse)
                .toList();

        AdminInventoryHealthResponse.Summary summary = AdminInventoryHealthResponse.Summary.builder()
                .expiredLots(totalExpiredLots)
                .expiringLots(totalExpiringLots)
                .qtyAtRisk(toDouble(totalQtyAtRisk))
                .unknownExpiryLots(totalUnknownExpiryLots)
                .build();

        return AdminInventoryHealthResponse.builder()
                .asOfDate(today)
                .windowDays(safeWindowDays)
                .includeExpiring(includeExpiringValue)
                .summary(summary)
                .farms(farms)
                .build();
    }

    private AdminInventoryHealthResponse.FarmHealth toFarmHealthResponse(FarmHealthAccumulator accumulator) {
        List<AdminInventoryHealthResponse.RiskLot> topLots = accumulator.topRiskLots.stream()
                .sorted(Comparator
                        .comparingInt((AdminInventoryHealthResponse.RiskLot lot) -> riskPriority(lot.getStatus()))
                        .thenComparing(
                                lot -> lot.getExpiryDate() == null ? LocalDate.MAX : LocalDate.parse(lot.getExpiryDate())))
                .limit(3)
                .toList();

        return AdminInventoryHealthResponse.FarmHealth.builder()
                .farmId(accumulator.farmId)
                .farmName(accumulator.farmName)
                .expiredLots(accumulator.expiredLots)
                .expiringLots(accumulator.expiringLots)
                .qtyAtRisk(toDouble(accumulator.qtyAtRisk))
                .topRiskLots(topLots)
                .build();
    }

    private int riskPriority(String status) {
        if ("EXPIRED".equals(status)) {
            return 1;
        }
        if ("EXPIRING".equals(status)) {
            return 2;
        }
        if ("UNKNOWN_EXPIRY".equals(status)) {
            return 3;
        }
        return 4;
    }

    private AdminInventoryMovementResponse toMovementResponse(org.example.QuanLyMuaVu.module.inventory.entity.StockMovement movement) {
        return AdminInventoryMovementResponse.builder()
                .movementId(movement.getId())
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .quantity(toDouble(movement.getQuantity()))
                .movementDate(movement.getMovementDate())
                .reference(resolveMovementReference(movement))
                .note(movement.getNote())
                .build();
    }

    private String resolveMovementReference(org.example.QuanLyMuaVu.module.inventory.entity.StockMovement movement) {
        if (movement.getTask() != null) {
            return "TASK-" + movement.getTask().getId();
        }
        if (movement.getSeason() != null) {
            return "SEASON-" + movement.getSeason().getId();
        }
        return null;
    }

    private List<LotAggregate> buildLotAggregates() {
        Map<Integer, LotAggregate> aggregateMap = new LinkedHashMap<>();
        for (org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance balance : inventoryQueryPort.findAllInventoryBalancesWithDetails()) {
            if (balance.getSupplyLot() == null) {
                continue;
            }
            Integer lotId = balance.getSupplyLot().getId();
            if (lotId == null) {
                continue;
            }

            LotAggregate aggregate = aggregateMap.computeIfAbsent(lotId, ignored -> new LotAggregate(balance.getSupplyLot()));
            aggregate.addBalance(balance);
        }

        return new ArrayList<>(aggregateMap.values());
    }

    private org.example.QuanLyMuaVu.module.farm.entity.Farm resolveFarmForResult(LotAggregate aggregate, Integer farmIdFilter) {
        if (farmIdFilter != null) {
            return aggregate.farmsById.get(farmIdFilter);
        }
        return aggregate.farmsById.values().stream().findFirst().orElse(null);
    }

    private String resolveRiskStatus(
            LocalDate expiryDate,
            LocalDate today,
            LocalDate cutoff,
            BigDecimal onHand,
            BigDecimal lowStockThreshold,
            boolean abnormalMovement) {

        if (expiryDate == null) {
            return abnormalMovement ? "ABNORMAL_MOVEMENT" : "UNKNOWN_EXPIRY";
        }
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        if (abnormalMovement) {
            return "ABNORMAL_MOVEMENT";
        }
        if (!expiryDate.isAfter(cutoff)) {
            return "EXPIRING";
        }
        if (lowStockThreshold != null && onHand.compareTo(lowStockThreshold) <= 0) {
            return "LOW_STOCK";
        }
        return "HEALTHY";
    }

    private String resolveSeverity(String status, LocalDate expiryDate, LocalDate today) {
        if ("EXPIRED".equals(status)) {
            return "CRITICAL";
        }
        if ("ABNORMAL_MOVEMENT".equals(status)) {
            return "HIGH";
        }
        if ("EXPIRING".equals(status)) {
            long days = expiryDate != null ? ChronoUnit.DAYS.between(today, expiryDate) : Long.MAX_VALUE;
            return days <= 7 ? "HIGH" : "MEDIUM";
        }
        if ("LOW_STOCK".equals(status)) {
            return "MEDIUM";
        }
        if ("UNKNOWN_EXPIRY".equals(status)) {
            return "LOW";
        }
        return "NONE";
    }

    private boolean matchesSeverityFilter(String severityFilter, String currentSeverity) {
        if (severityFilter == null || severityFilter.isBlank() || "ALL".equals(severityFilter)) {
            return true;
        }
        return severityFilter.equals(currentSeverity);
    }

    private boolean hasAbnormalMovement(
            Integer lotId,
            BigDecimal onHand,
            int windowDays,
            LocalDateTime now) {
        if (lotId == null) {
            return false;
        }

        LocalDateTime thresholdDate = now.minusDays(Math.max(windowDays, 1));
        List<org.example.QuanLyMuaVu.module.inventory.entity.StockMovement> movements = inventoryQueryPort
                .findStockMovementsBySupplyLotId(lotId);
        if (movements.isEmpty()) {
            return false;
        }

        long recentMovementCount = movements.stream()
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .count();

        long recentAdjustCount = movements.stream()
                .filter(movement -> movement.getMovementType() == org.example.QuanLyMuaVu.Enums.StockMovementType.ADJUST)
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .count();

        BigDecimal largeAdjustThreshold = onHand != null
                ? onHand.abs().multiply(BigDecimal.valueOf(0.5))
                : BigDecimal.ZERO;
        boolean hasLargeAdjust = movements.stream()
                .filter(movement -> movement.getMovementType() == org.example.QuanLyMuaVu.Enums.StockMovementType.ADJUST)
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .map(org.example.QuanLyMuaVu.module.inventory.entity.StockMovement::getQuantity)
                .filter(quantity -> quantity != null)
                .anyMatch(quantity -> quantity.compareTo(largeAdjustThreshold) >= 0);

        return recentAdjustCount >= 2 || hasLargeAdjust || recentMovementCount >= 8;
    }

    private String resolveHealthStatus(
            LocalDate expiryDate,
            LocalDate today,
            LocalDate cutoff,
            boolean includeExpiring) {

        if (expiryDate == null) {
            return "UNKNOWN_EXPIRY";
        }
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        if (includeExpiring && !expiryDate.isAfter(cutoff)) {
            return "EXPIRING";
        }
        return "HEALTHY";
    }

    private boolean matchesStatusFilter(String statusFilter, String currentStatus) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equals(statusFilter)) {
            return true;
        }
        if ("RISK".equals(statusFilter)) {
            return !"HEALTHY".equals(currentStatus);
        }
        if ("EXPIRING".equals(statusFilter)) {
            return "EXPIRING".equals(currentStatus);
        }
        return statusFilter.equals(currentStatus);
    }

    private Comparator<AdminInventoryRiskLotResponse> buildSortComparator(String sort) {
        String sortKey = sort != null ? sort.trim().toUpperCase(Locale.ROOT) : "EXPIRY_ASC";

        Comparator<AdminInventoryRiskLotResponse> comparator;
        switch (sortKey) {
            case "ONHAND_DESC":
                comparator = Comparator.comparing(AdminInventoryRiskLotResponse::getOnHand, Comparator.nullsLast(Double::compareTo))
                        .reversed();
                break;
            case "EXPIRY_DESC":
                comparator = Comparator.comparing(
                        this::parseExpiryForDesc,
                        Comparator.reverseOrder());
                break;
            case "EXPIRY_ASC":
            default:
                comparator = Comparator.comparing(this::parseExpiryForAsc);
                break;
        }

        return comparator.thenComparing(AdminInventoryRiskLotResponse::getLotId);
    }

    private LocalDate parseExpiryForAsc(AdminInventoryRiskLotResponse row) {
        if (row.getExpiryDate() == null) {
            return LocalDate.MAX;
        }
        return LocalDate.parse(row.getExpiryDate());
    }

    private LocalDate parseExpiryForDesc(AdminInventoryRiskLotResponse row) {
        if (row.getExpiryDate() == null) {
            return LocalDate.MIN;
        }
        return LocalDate.parse(row.getExpiryDate());
    }

    private int normalizeWindowDays(Integer windowDays) {
        if (windowDays == null || windowDays <= 0) {
            return 30;
        }
        return Math.min(windowDays, 365);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 200);
    }

    private String buildLocationLabel(org.example.QuanLyMuaVu.module.inventory.entity.StockLocation location) {
        List<String> parts = new ArrayList<>();
        if (location.getZone() != null && !location.getZone().isBlank()) {
            parts.add(location.getZone());
        }
        if (location.getAisle() != null && !location.getAisle().isBlank()) {
            parts.add(location.getAisle());
        }
        if (location.getShelf() != null && !location.getShelf().isBlank()) {
            parts.add(location.getShelf());
        }
        if (location.getBin() != null && !location.getBin().isBlank()) {
            parts.add(location.getBin());
        }
        return parts.isEmpty() ? "Location " + location.getId() : String.join("-", parts);
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0d;
    }

    private static class LotAggregate {
        private final org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot lot;
        private BigDecimal onHand = BigDecimal.ZERO;
        private final Map<Integer, org.example.QuanLyMuaVu.module.farm.entity.Farm> farmsById = new LinkedHashMap<>();

        private LotAggregate(org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot lot) {
            this.lot = lot;
        }

        private void addBalance(org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance balance) {
            BigDecimal quantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
            onHand = onHand.add(quantity);

            if (balance.getWarehouse() != null && balance.getWarehouse().getFarm() != null) {
                org.example.QuanLyMuaVu.module.farm.entity.Farm farm = balance.getWarehouse().getFarm();
                if (farm.getId() != null) {
                    farmsById.putIfAbsent(farm.getId(), farm);
                }
            }
        }
    }

    private static class FarmHealthAccumulator {
        private final Integer farmId;
        private final String farmName;
        private int expiredLots = 0;
        private int expiringLots = 0;
        private BigDecimal qtyAtRisk = BigDecimal.ZERO;
        private final List<AdminInventoryHealthResponse.RiskLot> topRiskLots = new ArrayList<>();

        private FarmHealthAccumulator(Integer farmId, String farmName) {
            this.farmId = farmId;
            this.farmName = farmName;
        }

        private int getExpiredLots() {
            return expiredLots;
        }

        private int getExpiringLots() {
            return expiringLots;
        }
    }
}
