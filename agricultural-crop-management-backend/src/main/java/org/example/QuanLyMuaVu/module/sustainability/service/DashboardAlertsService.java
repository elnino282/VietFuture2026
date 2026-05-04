package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentQueryPort;
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryLowStockView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for Dashboard alerts aggregation.
 * Single Responsibility: Alert computation and low stock detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardAlertsService {

    private final CurrentUserService currentUserService;
    private final IncidentQueryPort incidentQueryPort;
    private final InventoryQueryPort inventoryQueryPort;
    private final FarmQueryPort farmQueryPort;
    private final SustainabilityDashboardService sustainabilityDashboardService;

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int CRITICAL_LOW_STOCK_THRESHOLD = 2;
    private static final int EXPIRY_WINDOW_DAYS = 30;

    /**
     * Build alerts summary for owner.
     */
    public DashboardOverviewResponse.Alerts buildAlerts(Long ownerId) {
        long openIncidents = incidentQueryPort.countOpenIncidentsByOwnerId(ownerId);

        int expiringLots = (int) inventoryQueryPort.countExpiringLotsByOwnerId(
                ownerId,
                LocalDate.now().plusDays(EXPIRY_WINDOW_DAYS));

        int lowStockCount = inventoryQueryPort
                .findLowStockByOwnerId(ownerId, 100, BigDecimal.valueOf(LOW_STOCK_THRESHOLD))
                .size();

        return DashboardOverviewResponse.Alerts.builder()
                .openIncidents((int) openIncidents)
                .expiringLots(expiringLots)
                .lowStockItems(lowStockCount)
                .build();
    }

    public DashboardOverviewResponse.InventoryRisk buildInventoryRisk(Long ownerId) {
        List<OwnerLotSnapshot> lots = collectOwnerLots(ownerId);
        if (lots.isEmpty()) {
            return DashboardOverviewResponse.InventoryRisk.builder()
                    .atRiskLots(0)
                    .lowStockLots(0)
                    .criticalLowStockLots(0)
                    .expiringSoonLots(0)
                    .expiredLots(0)
                    .build();
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryWindow = today.plusDays(EXPIRY_WINDOW_DAYS);

        int atRiskLots = 0;
        int lowStockLots = 0;
        int criticalLowStockLots = 0;
        int expiringSoonLots = 0;
        int expiredLots = 0;

        for (OwnerLotSnapshot lot : lots) {
            boolean lowStock = isLowStock(lot.onHand(), LOW_STOCK_THRESHOLD);
            boolean criticalLowStock = isLowStock(lot.onHand(), CRITICAL_LOW_STOCK_THRESHOLD);
            boolean expiringSoon = lot.expiryDate() != null
                    && !lot.expiryDate().isBefore(today)
                    && !lot.expiryDate().isAfter(expiryWindow);
            boolean expired = lot.expiryDate() != null && lot.expiryDate().isBefore(today);

            if (lowStock) {
                lowStockLots++;
            }
            if (criticalLowStock) {
                criticalLowStockLots++;
            }
            if (expiringSoon) {
                expiringSoonLots++;
            }
            if (expired) {
                expiredLots++;
            }

            if (lowStock || expiringSoon || expired) {
                atRiskLots++;
            }
        }

        return DashboardOverviewResponse.InventoryRisk.builder()
                .atRiskLots(atRiskLots)
                .lowStockLots(lowStockLots)
                .criticalLowStockLots(criticalLowStockLots)
                .expiringSoonLots(expiringSoonLots)
                .expiredLots(expiredLots)
                .build();
    }

    public DashboardOverviewResponse.LotStatus buildLotStatus(Long ownerId) {
        List<OwnerLotSnapshot> lots = collectOwnerLots(ownerId);
        if (lots.isEmpty()) {
            return DashboardOverviewResponse.LotStatus.builder()
                    .totalLotsWithStock(0)
                    .activeLots(0)
                    .expiringSoonLots(0)
                    .expiredLots(0)
                    .unknownExpiryLots(0)
                    .byStatus(Map.of())
                    .build();
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryWindow = today.plusDays(EXPIRY_WINDOW_DAYS);

        int expiredLots = 0;
        int expiringSoonLots = 0;
        int unknownExpiryLots = 0;
        Map<String, Integer> byStatus = new LinkedHashMap<>();

        for (OwnerLotSnapshot lot : lots) {
            byStatus.merge(normalizeStatus(lot.status()), 1, Integer::sum);
            if (lot.expiryDate() == null) {
                unknownExpiryLots++;
                continue;
            }
            if (lot.expiryDate().isBefore(today)) {
                expiredLots++;
                continue;
            }
            if (!lot.expiryDate().isAfter(expiryWindow)) {
                expiringSoonLots++;
            }
        }

        int totalLots = lots.size();
        int activeLots = Math.max(totalLots - expiredLots, 0);

        return DashboardOverviewResponse.LotStatus.builder()
                .totalLotsWithStock(totalLots)
                .activeLots(activeLots)
                .expiringSoonLots(expiringSoonLots)
                .expiredLots(expiredLots)
                .unknownExpiryLots(unknownExpiryLots)
                .byStatus(byStatus)
                .build();
    }

    public DashboardOverviewResponse.SustainabilityAlerts buildSustainabilityAlerts(Integer seasonId) {
        List<FieldMapResponse.FieldMapItem> items = sustainabilityDashboardService
                .getFieldMap(seasonId, null, null, null)
                .getItems();

        if (items == null || items.isEmpty()) {
            return DashboardOverviewResponse.SustainabilityAlerts.builder()
                    .totalFields(0)
                    .highRiskFields(0)
                    .mediumRiskFields(0)
                    .lowRiskFields(0)
                    .fieldsMissingInputs(0)
                    .build();
        }

        int high = 0;
        int medium = 0;
        int low = 0;
        int missingInputs = 0;

        for (FieldMapResponse.FieldMapItem item : items) {
            String level = item.getFdnLevel() != null ? item.getFdnLevel().trim().toLowerCase(Locale.ROOT) : "";
            if ("high".equals(level)) {
                high++;
            } else if ("medium".equals(level)) {
                medium++;
            } else if ("low".equals(level)) {
                low++;
            }

            if (item.getMissingInputs() != null && !item.getMissingInputs().isEmpty()) {
                missingInputs++;
            }
        }

        return DashboardOverviewResponse.SustainabilityAlerts.builder()
                .totalFields(items.size())
                .highRiskFields(high)
                .mediumRiskFields(medium)
                .lowRiskFields(low)
                .fieldsMissingInputs(missingInputs)
                .build();
    }

    /**
     * Get low stock alerts.
     */
    public List<LowStockAlertResponse> getLowStock(int limit) {
        Long ownerId = currentUserService.getCurrentUserId();
        if (ownerId == null || limit <= 0) {
            return List.of();
        }

        return inventoryQueryPort.findLowStockByOwnerId(ownerId, limit, BigDecimal.valueOf(LOW_STOCK_THRESHOLD))
                .stream()
                .map(this::toLowStockAlertResponse)
                .toList();
    }

    private LowStockAlertResponse toLowStockAlertResponse(InventoryLowStockView item) {
        return LowStockAlertResponse.builder()
                .supplyLotId(item.supplyLotId())
                .batchCode(item.batchCode())
                .itemName(item.itemName())
                .warehouseName(item.warehouseName())
                .locationLabel(item.locationLabel())
                .onHand(item.onHand())
                .unit(item.unit())
                .build();
    }

    private List<OwnerLotSnapshot> collectOwnerLots(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }

        Set<Integer> farmIds = farmQueryPort.findFarmsByOwnerId(ownerId).stream()
                .map(org.example.QuanLyMuaVu.module.farm.entity.Farm::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (farmIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, OwnerLotAccumulator> byLotId = new LinkedHashMap<>();
        for (InventoryBalance balance : inventoryQueryPort.findAllInventoryBalancesWithDetails()) {
            if (balance == null || balance.getSupplyLot() == null || balance.getWarehouse() == null
                    || balance.getWarehouse().getFarm() == null || balance.getWarehouse().getFarm().getId() == null) {
                continue;
            }
            if (!farmIds.contains(balance.getWarehouse().getFarm().getId())) {
                continue;
            }

            SupplyLot lot = balance.getSupplyLot();
            if (lot.getId() == null) {
                continue;
            }

            OwnerLotAccumulator acc = byLotId.computeIfAbsent(
                    lot.getId(),
                    ignored -> new OwnerLotAccumulator(lot.getId(), lot.getStatus(), lot.getExpiryDate()));
            acc.onHand = acc.onHand.add(balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO);
        }

        return byLotId.values().stream()
                .map(OwnerLotAccumulator::toSnapshot)
                .filter(snapshot -> snapshot.onHand().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private boolean isLowStock(BigDecimal onHand, int threshold) {
        if (onHand == null) {
            return false;
        }
        return onHand.compareTo(BigDecimal.ZERO) > 0 && onHand.compareTo(BigDecimal.valueOf(threshold)) <= 0;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private record OwnerLotSnapshot(Integer lotId, String status, LocalDate expiryDate, BigDecimal onHand) {
    }

    private static final class OwnerLotAccumulator {
        private final Integer lotId;
        private final String status;
        private final LocalDate expiryDate;
        private BigDecimal onHand = BigDecimal.ZERO;

        private OwnerLotAccumulator(Integer lotId, String status, LocalDate expiryDate) {
            this.lotId = lotId;
            this.status = status;
            this.expiryDate = expiryDate;
        }

        private OwnerLotSnapshot toSnapshot() {
            return new OwnerLotSnapshot(lotId, status, expiryDate, onHand);
        }
    }
}
