package org.example.adminreporting.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.AdminInventoryHealthResponse;
import org.example.adminreporting.dto.response.AdminInventoryLotDetailResponse;
import org.example.adminreporting.dto.response.AdminInventoryMovementResponse;
import org.example.adminreporting.dto.response.AdminInventoryOptionsResponse;
import org.example.adminreporting.dto.response.AdminInventoryRiskLotResponse;
import org.example.adminreporting.entity.FarmSummary;
import org.example.adminreporting.entity.InventoryLotSummary;
import org.example.adminreporting.repository.FarmSummaryRepository;
import org.example.adminreporting.repository.InventoryLotSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminInventoryService {

    private final InventoryLotSummaryRepository inventoryLotSummaryRepository;
    private final FarmSummaryRepository farmSummaryRepository;

    public AdminInventoryOptionsResponse getOptions() {
        List<AdminInventoryOptionsResponse.FarmOption> farms = farmSummaryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(FarmSummary::getFarmName, String.CASE_INSENSITIVE_ORDER))
                .map(farm -> AdminInventoryOptionsResponse.FarmOption.builder()
                        .id(farm.getFarmId())
                        .name(farm.getFarmName())
                        .build())
                .toList();

        List<String> categories = List.of("EXPIRED", "EXPIRING_SOON", "NORMAL");

        List<AdminInventoryOptionsResponse.ItemOption> items = List.of(
                AdminInventoryOptionsResponse.ItemOption.builder()
                        .id(1)
                        .name("Default Item")
                        .build()
        );

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
            java.math.BigDecimal lowStockThreshold,
            Integer page,
            Integer limit) {

        int safeWindowDays = windowDays == null || windowDays <= 0 ? 30 : Math.min(windowDays, 365);
        int safePage = Math.max(page != null ? page : 0, 0);
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        String statusFilter = status != null ? status.trim().toUpperCase(Locale.ROOT) : "RISK";
        String severityFilter = severity != null ? severity.trim().toUpperCase(Locale.ROOT) : "ALL";
        String searchKeyword = q != null ? q.trim().toLowerCase(Locale.ROOT) : null;

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(safeWindowDays);

        List<InventoryLotSummary> lots = inventoryLotSummaryRepository.findAll();
        List<AdminInventoryRiskLotResponse> filtered = new ArrayList<>();

        for (InventoryLotSummary lot : lots) {
            if (farmId != null && !farmId.equals(lot.getFarmId())) {
                continue;
            }

            if (searchKeyword != null && !searchKeyword.isBlank()) {
                String farmNameLower = lot.getFarmName() != null ? lot.getFarmName().toLowerCase(Locale.ROOT) : "";
                String lotIdStr = String.valueOf(lot.getLotId());
                if (!farmNameLower.contains(searchKeyword) && !lotIdStr.contains(searchKeyword)) {
                    continue;
                }
            }

            LocalDate expiry = lot.getExpiryDate();
            boolean expired = expiry != null && expiry.isBefore(today);
            boolean expiringSoon = expiry != null && !expiry.isBefore(today) && !expiry.isAfter(cutoff);
            boolean hasRisk = expired || expiringSoon;

            String riskStatus = "NORMAL";
            if (expired) {
                riskStatus = "EXPIRED";
            } else if (expiringSoon) {
                riskStatus = "EXPIRING_SOON";
            }

            if ("RISK".equals(statusFilter) && !hasRisk) {
                continue;
            } else if (!"RISK".equals(statusFilter) && !"ALL".equals(statusFilter) && !statusFilter.equals(riskStatus)) {
                continue;
            }

            String riskSeverity = "LOW";
            if (expired) {
                riskSeverity = "HIGH";
            } else if (expiringSoon) {
                long days = ChronoUnit.DAYS.between(today, expiry);
                if (days <= 7) {
                    riskSeverity = "HIGH";
                } else if (days <= 15) {
                    riskSeverity = "MEDIUM";
                }
            }

            if (!"ALL".equals(severityFilter) && !severityFilter.equals(riskSeverity)) {
                continue;
            }

            Long daysToExpiry = expiry != null ? ChronoUnit.DAYS.between(today, expiry) : null;

            filtered.add(AdminInventoryRiskLotResponse.builder()
                    .lotId(lot.getLotId())
                    .itemId(1)
                    .itemName("Lot " + lot.getLotId())
                    .lotCode("LOT-" + lot.getLotId())
                    .farmId(lot.getFarmId())
                    .farmName(lot.getFarmName())
                    .expiryDate(expiry != null ? expiry.toString() : null)
                    .onHand(10.0)
                    .daysToExpiry(daysToExpiry)
                    .status(riskStatus)
                    .severity(riskSeverity)
                    .unit("KG")
                    .unitCost(1.0)
                    .build());
        }

        // Apply simple sorting
        if ("expiryDate".equals(sort)) {
            filtered.sort(Comparator.comparing(AdminInventoryRiskLotResponse::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("daysToExpiry".equals(sort)) {
            filtered.sort(Comparator.comparing(AdminInventoryRiskLotResponse::getDaysToExpiry, Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            filtered.sort(Comparator.comparing(AdminInventoryRiskLotResponse::getLotId));
        }

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
        InventoryLotSummary lot = inventoryLotSummaryRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Supply lot not found with ID: " + lotId));

        List<AdminInventoryLotDetailResponse.BalanceRow> balances = List.of(
                AdminInventoryLotDetailResponse.BalanceRow.builder()
                        .warehouseId(1)
                        .warehouseName("Main Warehouse")
                        .farmId(lot.getFarmId())
                        .farmName(lot.getFarmName())
                        .locationId(1)
                        .locationLabel("Section A")
                        .quantity(10.0)
                        .build()
        );

        return AdminInventoryLotDetailResponse.builder()
                .lotId(lot.getLotId())
                .itemId(1)
                .itemName("Lot " + lot.getLotId())
                .lotCode("LOT-" + lot.getLotId())
                .unit("KG")
                .supplierName("Default Supplier")
                .expiryDate(lot.getExpiryDate() != null ? lot.getExpiryDate().toString() : null)
                .status("NORMAL")
                .onHandTotal(10.0)
                .balances(balances)
                .build();
    }

    public List<AdminInventoryMovementResponse> getLotMovements(Integer lotId) {
        if (!inventoryLotSummaryRepository.existsById(lotId)) {
            throw new IllegalArgumentException("Supply lot not found with ID: " + lotId);
        }

        return List.of(
                AdminInventoryMovementResponse.builder()
                        .movementId(1)
                        .movementType("RECEIVE")
                        .quantity(10.0)
                        .movementDate(LocalDateTime.now().minusDays(1))
                        .reference("REF-INITIAL")
                        .note("Initial ingestion backfilled")
                        .build()
        );
    }

    public AdminInventoryHealthResponse getInventoryHealth(Integer windowDays, Boolean includeExpiring, Integer limit) {
        int safeWindowDays = windowDays == null || windowDays <= 0 ? 30 : Math.min(windowDays, 365);
        boolean includeExpiringValue = includeExpiring == null || includeExpiring;
        int safeLimit = limit == null || limit <= 0 ? 5 : Math.min(limit, 50);

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(safeWindowDays);

        List<InventoryLotSummary> lots = inventoryLotSummaryRepository.findAll();

        int totalExpiredLots = 0;
        int totalExpiringSoonLots = 0;
        int missingExpiryDateCount = 0;
        int totalLotsEvaluated = 0;

        Set<Integer> affectedFarmIds = new HashSet<>();
        Map<Integer, FarmHealthAccumulator> farmAccumulatorMap = new LinkedHashMap<>();

        for (InventoryLotSummary lot : lots) {
            totalLotsEvaluated++;
            boolean missingExpiryDate = lot.getExpiryDate() == null;
            if (missingExpiryDate) {
                missingExpiryDateCount++;
                continue; // No risk calculation possible without expiry date
            }

            boolean expired = lot.getExpiryDate().isBefore(today);
            boolean expiringSoon = includeExpiringValue
                    && !lot.getExpiryDate().isBefore(today)
                    && !lot.getExpiryDate().isAfter(cutoff);

            boolean hasRisk = expired || expiringSoon;

            if (expired) {
                totalExpiredLots++;
            }
            if (expiringSoon) {
                totalExpiringSoonLots++;
            }

            if (hasRisk) {
                Integer farmId = lot.getFarmId();
                String farmName = lot.getFarmName();
                affectedFarmIds.add(farmId);

                FarmHealthAccumulator farmAccumulator = farmAccumulatorMap.computeIfAbsent(
                        farmId,
                        ignored -> new FarmHealthAccumulator(farmId, farmName));

                if (expired) {
                    farmAccumulator.expiredLots++;
                }
                if (expiringSoon) {
                    farmAccumulator.expiringSoonLots++;
                }

                String lotStatus = expired ? "EXPIRED" : "EXPIRING_SOON";

                farmAccumulator.topRiskLots.add(AdminInventoryHealthResponse.RiskLot.builder()
                        .lotId(lot.getLotId())
                        .itemName("Lot " + lot.getLotId()) // read-only fallback name
                        .expiryDate(lot.getExpiryDate().toString())
                        .onHand(1.0) // fallback value since we only have IDs in read-model summary
                        .status(lotStatus)
                        .build());
            }
        }

        List<AdminInventoryHealthResponse.FarmHealth> farms = farmAccumulatorMap.values()
                .stream()
                .sorted(Comparator
                        .comparingInt(FarmHealthAccumulator::getExpiredLots).reversed()
                        .thenComparingInt(FarmHealthAccumulator::getExpiringSoonLots).reversed())
                .limit(safeLimit)
                .map(this::toFarmHealthResponse)
                .toList();

        Double coveragePercent = totalLotsEvaluated > 0
                ? Math.round(((double) (totalLotsEvaluated - missingExpiryDateCount) / totalLotsEvaluated) * 10000.0) / 100.0
                : null;

        AdminInventoryHealthResponse.Summary summary = AdminInventoryHealthResponse.Summary.builder()
                .expiredLots(totalExpiredLots)
                .expiringSoonLots(totalExpiringSoonLots)
                .expiringLots(totalExpiringSoonLots)
                .lowStockLots(0)
                .noMovementLots(0)
                .slowMovementLots(0)
                .qtyAtRisk((double) (totalExpiredLots + totalExpiringSoonLots))
                .totalAffectedFarms(affectedFarmIds.size())
                .totalAffectedItems(0)
                .unknownExpiryLots(missingExpiryDateCount)
                .build();

        AdminInventoryHealthResponse.DataQuality dataQuality = AdminInventoryHealthResponse.DataQuality.builder()
                .missingExpiryDateCount(missingExpiryDateCount)
                .missingMovementHistoryCount(0)
                .coveragePercent(coveragePercent)
                .build();

        return AdminInventoryHealthResponse.builder()
                .asOfDate(today)
                .windowDays(safeWindowDays)
                .includeExpiring(includeExpiringValue)
                .summary(summary)
                .dataQuality(dataQuality)
                .farms(farms)
                .build();
    }

    private AdminInventoryHealthResponse.FarmHealth toFarmHealthResponse(FarmHealthAccumulator accumulator) {
        List<AdminInventoryHealthResponse.RiskLot> topLots = accumulator.topRiskLots.stream()
                .sorted(Comparator
                        .comparingInt((AdminInventoryHealthResponse.RiskLot lot) -> riskPriority(lot.getStatus()))
                        .thenComparing(lot -> lot.getExpiryDate() == null ? LocalDate.MAX : LocalDate.parse(lot.getExpiryDate())))
                .limit(3)
                .toList();

        return AdminInventoryHealthResponse.FarmHealth.builder()
                .farmId(accumulator.farmId)
                .farmName(accumulator.farmName)
                .expiredLots(accumulator.expiredLots)
                .expiringSoonLots(accumulator.expiringSoonLots)
                .expiringLots(accumulator.expiringSoonLots)
                .lowStockLots(0)
                .noMovementLots(0)
                .slowMovementLots(0)
                .qtyAtRisk((double) (accumulator.expiredLots + accumulator.expiringSoonLots))
                .topRiskLots(topLots)
                .build();
    }

    private int riskPriority(String status) {
        if ("EXPIRED".equals(status)) {
            return 1;
        }
        if ("EXPIRING_SOON".equals(status)) {
            return 2;
        }
        return 3;
    }

    private static class FarmHealthAccumulator {
        private final Integer farmId;
        private final String farmName;
        private int expiredLots = 0;
        private int expiringSoonLots = 0;
        private final List<AdminInventoryHealthResponse.RiskLot> topRiskLots = new ArrayList<>();

        private FarmHealthAccumulator(Integer farmId, String farmName) {
            this.farmId = farmId;
            this.farmName = farmName;
        }

        private int getExpiredLots() {
            return expiredLots;
        }

        private int getExpiringSoonLots() {
            return expiringSoonLots;
        }
    }
}
