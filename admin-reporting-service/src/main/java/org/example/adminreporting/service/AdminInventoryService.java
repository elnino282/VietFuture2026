package org.example.adminreporting.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.response.AdminInventoryHealthResponse;
import org.example.adminreporting.entity.InventoryLotSummary;
import org.example.adminreporting.repository.InventoryLotSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminInventoryService {

    private final InventoryLotSummaryRepository inventoryLotSummaryRepository;

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
