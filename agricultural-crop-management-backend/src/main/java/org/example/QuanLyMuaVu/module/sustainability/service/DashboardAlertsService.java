package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.port.IncidentQueryPort;
import org.example.QuanLyMuaVu.module.inventory.dto.response.DashboardInventoryAlertsResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryLowStockView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardIncidentAlertResponse;
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

    private static final BigDecimal DEFAULT_LOW_STOCK_THRESHOLD = BigDecimal.valueOf(5);
    private static final int DEFAULT_CRITICAL_LOW_STOCK_THRESHOLD = 2;
    private static final int DEFAULT_EXPIRY_WINDOW_DAYS = 30;
    private static final int DEFAULT_NO_MOVEMENT_DAYS = 30;
    private static final int DEFAULT_ABNORMAL_ADJUST_COUNT = 2;
    private static final BigDecimal DEFAULT_ABNORMAL_ADJUST_RATIO = new BigDecimal("0.50");
    private static final int DEFAULT_ALERT_LIMIT = 20;
    private static final int MAX_ALERT_LIMIT = 100;
    private static final int DEFAULT_INCIDENT_ALERT_LIMIT = 30;
    private static final String THRESHOLD_SOURCE_BACKEND_CONFIG = "BACKEND_CONFIG";
    private static final String THRESHOLD_SOURCE_BACKEND_DEFAULT = "BACKEND_DEFAULT";
    private static final List<TaskStatus> TASK_DONE_STATUSES = List.of(TaskStatus.DONE, TaskStatus.CANCELLED);
    private static final String ACTION_TARGET_DISEASE_WORKSPACE = "DISEASE_WORKSPACE";
    private static final String ACTION_TARGET_TASK_WORKSPACE = "TASK_WORKSPACE";
    private static final String ACTION_TARGET_SEASON_REPORTS = "SEASON_REPORTS";

    private final CurrentUserService currentUserService;
    private final IncidentQueryPort incidentQueryPort;
    private final InventoryQueryPort inventoryQueryPort;
    private final FarmQueryPort farmQueryPort;
    private final TaskQueryPort taskQueryPort;
    private final SustainabilityDashboardService sustainabilityDashboardService;
    private final AppProperties appProperties;

    /**
     * Build alerts summary for owner.
     */
    public DashboardOverviewResponse.Alerts buildAlerts(Long ownerId) {
        InventoryAlertSettings settings = resolveInventoryAlertSettings();
        long openIncidents = incidentQueryPort.countOpenIncidentsByOwnerId(ownerId);

        int expiringLots = (int) inventoryQueryPort.countExpiringLotsByOwnerId(
                ownerId,
                LocalDate.now().plusDays(settings.expiringSoonDays()));

        int lowStockCount = inventoryQueryPort
                .findLowStockByOwnerId(ownerId, 100, settings.lowStockThreshold())
                .size();

        return DashboardOverviewResponse.Alerts.builder()
                .openIncidents((int) openIncidents)
                .expiringLots(expiringLots)
                .lowStockItems(lowStockCount)
                .build();
    }

    public DashboardOverviewResponse.InventoryRisk buildInventoryRisk(Long ownerId) {
        InventoryAlertSettings settings = resolveInventoryAlertSettings();
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
        LocalDate expiryWindow = today.plusDays(settings.expiringSoonDays());
        BigDecimal criticalThreshold = settings.lowStockThreshold()
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(DEFAULT_CRITICAL_LOW_STOCK_THRESHOLD));

        int atRiskLots = 0;
        int lowStockLots = 0;
        int criticalLowStockLots = 0;
        int expiringSoonLots = 0;
        int expiredLots = 0;

        for (OwnerLotSnapshot lot : lots) {
            boolean lowStock = isLowStock(lot.onHand(), settings.lowStockThreshold());
            boolean criticalLowStock = isLowStock(lot.onHand(), criticalThreshold);
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
        InventoryAlertSettings settings = resolveInventoryAlertSettings();
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
        LocalDate expiryWindow = today.plusDays(settings.expiringSoonDays());

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
        List<FieldMapResponse.FieldMapItem> items = collectMapItems(
                sustainabilityDashboardService.getFieldMap(seasonId, null, null, null));

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
        InventoryAlertSettings settings = resolveInventoryAlertSettings();
        Long ownerId = currentUserService.getCurrentUserId();
        if (ownerId == null || limit <= 0) {
            return List.of();
        }

        return inventoryQueryPort.findLowStockByOwnerId(ownerId, limit, settings.lowStockThreshold())
                .stream()
                .map(this::toLowStockAlertResponse)
                .toList();
    }

    public List<DashboardIncidentAlertResponse> getIncidentAlerts(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        if (ownerId == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<DashboardIncidentAlertResponse> alerts = new ArrayList<>();

        incidentQueryPort.findOpenIncidentsByOwnerId(ownerId, seasonId).stream()
                .map(this::toIncidentAlert)
                .filter(item -> item != null)
                .forEach(alerts::add);

        taskQueryPort.findOverdueTasksByUser(
                ownerId,
                seasonId,
                today,
                TASK_DONE_STATUSES,
                DEFAULT_INCIDENT_ALERT_LIMIT).stream()
                .map(task -> toOverdueTaskAlert(task, today))
                .filter(item -> item != null)
                .forEach(alerts::add);

        FieldMapResponse riskItems = sustainabilityDashboardService
                .getFieldMap(seasonId, null, null, null);
        List<FieldMapResponse.FieldMapItem> mapItems = collectMapItems(riskItems);
        if (!mapItems.isEmpty()) {
            mapItems.stream()
                    .map(item -> toSeasonRiskAlert(item, seasonId))
                    .filter(item -> item != null)
                    .forEach(alerts::add);
        }

        return alerts.stream()
                .sorted(Comparator
                        .comparingInt((DashboardIncidentAlertResponse item) -> severityRank(item.getSeverity()))
                        .thenComparing(
                                DashboardIncidentAlertResponse::getDueDate,
                                Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(
                                DashboardIncidentAlertResponse::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DEFAULT_INCIDENT_ALERT_LIMIT)
                .toList();
    }

    public DashboardInventoryAlertsResponse getInventoryAlerts(int limit) {
        InventoryAlertSettings settings = resolveInventoryAlertSettings();
        int safeLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now();
        LocalDate expiringCutoff = today.plusDays(settings.expiringSoonDays());
        LocalDateTime now = LocalDateTime.now();

        Long ownerId = currentUserService.getCurrentUserId();
        if (ownerId == null) {
            return emptyInventoryAlertsResponse(today, settings);
        }

        Set<Integer> ownerFarmIds = resolveOwnerFarmIds(ownerId);
        if (ownerFarmIds.isEmpty()) {
            return emptyInventoryAlertsResponse(today, settings);
        }

        List<LotInventorySnapshot> lotSnapshots = collectLotInventorySnapshots(ownerFarmIds);
        if (lotSnapshots.isEmpty()) {
            return emptyInventoryAlertsResponse(today, settings);
        }

        Map<Integer, List<StockMovement>> movementCache = new HashMap<>();
        List<DashboardInventoryAlertsResponse.AlertItem> alerts = new ArrayList<>();
        int lowStockCount = 0;
        int expiredCount = 0;
        int expiringSoonCount = 0;
        int noMovementCount = 0;
        int abnormalMovementCount = 0;

        for (LotInventorySnapshot snapshot : lotSnapshots) {
            List<StockMovement> movements = movementCache.computeIfAbsent(
                    snapshot.lotId(),
                    inventoryQueryPort::findStockMovementsBySupplyLotId);
            LocalDateTime lastMovementAt = resolveLastMovementAt(movements);
            boolean hasMovements = lastMovementAt != null;

            if (snapshot.expiryDate() != null && snapshot.expiryDate().isBefore(today)) {
                alerts.add(buildAlertItem(snapshot, "EXPIRED", "CRITICAL", null, lastMovementAt));
                expiredCount++;
            }

            if (snapshot.expiryDate() != null
                    && !snapshot.expiryDate().isBefore(today)
                    && !snapshot.expiryDate().isAfter(expiringCutoff)) {
                long daysToExpiry = ChronoUnit.DAYS.between(today, snapshot.expiryDate());
                String severity = daysToExpiry <= 7 ? "HIGH" : "MEDIUM";
                alerts.add(buildAlertItem(snapshot, "EXPIRING_SOON", severity, null, lastMovementAt));
                expiringSoonCount++;
            }

            if (isLowStock(snapshot.onHand(), settings.lowStockThreshold())) {
                String reason = settings.thresholdConfigured() ? null : "THRESHOLD_NOT_CONFIGURED";
                String severity = resolveLowStockSeverity(snapshot.onHand(), settings.lowStockThreshold());
                alerts.add(buildAlertItem(snapshot, "LOW_STOCK", severity, reason, lastMovementAt));
                lowStockCount++;
            }

            if (hasMovements && hasAbnormalMovement(
                    movements,
                    snapshot.onHand(),
                    now,
                    settings.noMovementDays(),
                    settings.abnormalAdjustCount(),
                    settings.abnormalAdjustRatio())) {
                alerts.add(buildAlertItem(snapshot, "ABNORMAL_MOVEMENT", "HIGH", null, lastMovementAt));
                abnormalMovementCount++;
            }

            if (hasMovements && lastMovementAt.isBefore(now.minusDays(settings.noMovementDays()))) {
                long staleDays = ChronoUnit.DAYS.between(lastMovementAt, now);
                String severity = staleDays >= settings.noMovementDays() * 2L ? "HIGH" : "MEDIUM";
                alerts.add(buildAlertItem(snapshot, "NO_MOVEMENT", severity, null, lastMovementAt));
                noMovementCount++;
            }
        }

        List<DashboardInventoryAlertsResponse.AlertItem> sorted = alerts.stream()
                .sorted(Comparator
                        .comparingInt((DashboardInventoryAlertsResponse.AlertItem item) -> severityRank(item.getSeverity()))
                        .thenComparing(item -> item.getExpiryDate() != null ? item.getExpiryDate() : LocalDate.MAX)
                        .thenComparing(item -> item.getItemName() != null ? item.getItemName() : ""))
                .limit(safeLimit)
                .toList();

        DashboardInventoryAlertsResponse.Summary summary = DashboardInventoryAlertsResponse.Summary.builder()
                .totalAlerts(alerts.size())
                .lowStock(lowStockCount)
                .expired(expiredCount)
                .expiringSoon(expiringSoonCount)
                .noMovement(noMovementCount)
                .abnormalMovement(abnormalMovementCount)
                .build();

        return DashboardInventoryAlertsResponse.builder()
                .asOfDate(today)
                .lowStockThreshold(settings.lowStockThreshold())
                .expiringSoonDays(settings.expiringSoonDays())
                .noMovementDays(settings.noMovementDays())
                .thresholdSource(settings.thresholdSource())
                .summary(summary)
                .alerts(sorted)
                .build();
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

    private DashboardIncidentAlertResponse toIncidentAlert(Incident incident) {
        if (incident == null) {
            return null;
        }

        String severity = incident.getSeverity() != null
                ? incident.getSeverity().name()
                : "MEDIUM";
        boolean isHighSeverity = incident.getSeverity() == IncidentSeverity.HIGH;
        Integer seasonId = incident.getSeasonId();
        Integer plotId = incident.getSeason() != null && incident.getSeason().getPlot() != null
                ? incident.getSeason().getPlot().getId()
                : null;
        String plotName = incident.getSeason() != null && incident.getSeason().getPlot() != null
                ? incident.getSeason().getPlot().getPlotName()
                : null;

        return DashboardIncidentAlertResponse.builder()
                .id("incident-" + incident.getId())
                .type(isHighSeverity ? "HIGH_SEVERITY_INCIDENT" : "OPEN_INCIDENT")
                .severity(severity)
                .title(isHighSeverity ? "High severity incident reported" : "Open incident requires attention")
                .description(buildIncidentDescription(incident, plotName))
                .seasonId(seasonId)
                .plotId(plotId)
                .createdAt(incident.getCreatedAt())
                .dueDate(incident.getDeadline())
                .actionUrl(resolveWorkspaceActionUrl(seasonId, "disease"))
                .actionTarget(ACTION_TARGET_DISEASE_WORKSPACE)
                .build();
    }

    private DashboardIncidentAlertResponse toOverdueTaskAlert(DashboardTaskView task, LocalDate today) {
        if (task == null || task.getTaskId() == null) {
            return null;
        }

        long overdueDays = task.getDueDate() != null ? ChronoUnit.DAYS.between(task.getDueDate(), today) : 0;
        String severity = overdueDays >= 7 ? "HIGH" : "MEDIUM";

        return DashboardIncidentAlertResponse.builder()
                .id("task-overdue-" + task.getTaskId())
                .type("OVERDUE_TASK")
                .severity(severity)
                .title("Overdue task: " + task.getTitle())
                .description(buildOverdueTaskDescription(task, overdueDays))
                .seasonId(task.getSeasonId())
                .plotId(task.getPlotId())
                .dueDate(task.getDueDate())
                .actionUrl(resolveWorkspaceActionUrl(task.getSeasonId(), "tasks"))
                .actionTarget(ACTION_TARGET_TASK_WORKSPACE)
                .build();
    }

    private DashboardIncidentAlertResponse toSeasonRiskAlert(FieldMapResponse.FieldMapItem item, Integer seasonId) {
        if (item == null || item.getFieldId() == null) {
            return null;
        }

        String level = item.getFdnLevel() != null
                ? item.getFdnLevel().trim().toLowerCase(Locale.ROOT)
                : "";
        List<String> missingInputs = item.getMissingInputs() != null ? item.getMissingInputs() : List.of();

        boolean hasMissingInputs = !missingInputs.isEmpty();
        boolean hasRiskLevel = "high".equals(level) || "medium".equals(level);

        if (!hasMissingInputs && !hasRiskLevel) {
            return null;
        }

        String severity = "high".equals(level) ? "HIGH" : "MEDIUM";
        String fieldName = item.getFieldName() != null ? item.getFieldName() : "Field #" + item.getFieldId();

        return DashboardIncidentAlertResponse.builder()
                .id("season-risk-" + item.getFieldId() + "-" + level + "-" + hasMissingInputs)
                .type(hasMissingInputs ? "SUSTAINABILITY_WARNING" : "SEASON_RISK")
                .severity(severity)
                .title(hasMissingInputs
                        ? "Sustainability warning on " + fieldName
                        : "Season risk on " + fieldName)
                .description(buildRiskDescription(level, missingInputs, item.getRecommendations()))
                .seasonId(seasonId)
                .plotId(item.getFieldId())
                .createdAt(LocalDateTime.now())
                .actionUrl(resolveWorkspaceActionUrl(seasonId, "reports"))
                .actionTarget(ACTION_TARGET_SEASON_REPORTS)
                .build();
    }

    private String buildIncidentDescription(Incident incident, String plotName) {
        String description = incident.getDescription() != null && !incident.getDescription().isBlank()
                ? incident.getDescription().trim()
                : "No description provided.";
        String plotSegment = plotName != null && !plotName.isBlank()
                ? " Plot: " + plotName + "."
                : "";
        return "Type: " + formatInputCode(incident.getIncidentType()) + "." + plotSegment + " " + description;
    }

    private String buildOverdueTaskDescription(DashboardTaskView task, long overdueDays) {
        String plotLabel = task.getPlotName() != null && !task.getPlotName().isBlank()
                ? task.getPlotName()
                : "Unknown plot";
        String overdueSegment = overdueDays > 0
                ? "Overdue by " + overdueDays + " day(s)."
                : "Task is overdue.";
        return "Plot: " + plotLabel + ". " + overdueSegment;
    }

    private String buildRiskDescription(String level, List<String> missingInputs, List<String> recommendations) {
        String riskLevel = level == null || level.isBlank() ? "unknown" : level;
        if (!missingInputs.isEmpty()) {
            String inputs = missingInputs.stream()
                    .map(this::formatInputCode)
                    .limit(3)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("N/A");
            return "Risk level: " + riskLevel + ". Missing inputs: " + inputs + ".";
        }

        if (recommendations != null && !recommendations.isEmpty()) {
            return "Risk level: " + riskLevel + ". " + recommendations.get(0);
        }

        return "Risk level: " + riskLevel + ".";
    }

    private String formatInputCode(String code) {
        if (code == null || code.isBlank()) {
            return "Unknown";
        }
        String[] tokens = code.trim().toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(token.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(token.substring(1));
        }
        return out.isEmpty() ? code : out.toString();
    }

    private String resolveWorkspaceActionUrl(Integer seasonId, String modulePath) {
        if (seasonId == null || modulePath == null || modulePath.isBlank()) {
            return "/farmer/dashboard";
        }
        return "/farmer/seasons/" + seasonId + "/workspace/" + modulePath;
    }

    private List<FieldMapResponse.FieldMapItem> collectMapItems(FieldMapResponse response) {
        if (response == null) {
            return List.of();
        }
        List<FieldMapResponse.FieldMapItem> items = new ArrayList<>();
        if (response.getFieldsWithBoundary() != null) {
            items.addAll(response.getFieldsWithBoundary());
        }
        if (response.getFieldsMissingBoundary() != null) {
            items.addAll(response.getFieldsMissingBoundary());
        }
        return items;
    }

    private DashboardInventoryAlertsResponse emptyInventoryAlertsResponse(
            LocalDate today,
            InventoryAlertSettings settings) {
        return DashboardInventoryAlertsResponse.builder()
                .asOfDate(today)
                .lowStockThreshold(settings.lowStockThreshold())
                .expiringSoonDays(settings.expiringSoonDays())
                .noMovementDays(settings.noMovementDays())
                .thresholdSource(settings.thresholdSource())
                .summary(DashboardInventoryAlertsResponse.Summary.builder()
                        .totalAlerts(0)
                        .lowStock(0)
                        .expired(0)
                        .expiringSoon(0)
                        .noMovement(0)
                        .abnormalMovement(0)
                        .build())
                .alerts(List.of())
                .build();
    }

    private List<OwnerLotSnapshot> collectOwnerLots(Long ownerId) {
        Set<Integer> farmIds = resolveOwnerFarmIds(ownerId);
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

    private List<LotInventorySnapshot> collectLotInventorySnapshots(Set<Integer> ownerFarmIds) {
        Map<Integer, LotInventoryAccumulator> byLotId = new LinkedHashMap<>();
        for (InventoryBalance balance : inventoryQueryPort.findAllInventoryBalancesWithDetails()) {
            if (balance == null || balance.getSupplyLot() == null || balance.getSupplyLot().getId() == null
                    || balance.getWarehouse() == null || balance.getWarehouse().getFarm() == null
                    || balance.getWarehouse().getFarm().getId() == null) {
                continue;
            }
            if (!ownerFarmIds.contains(balance.getWarehouse().getFarm().getId())) {
                continue;
            }
            BigDecimal quantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Integer lotId = balance.getSupplyLot().getId();
            LotInventoryAccumulator accumulator = byLotId.computeIfAbsent(
                    lotId,
                    ignored -> new LotInventoryAccumulator(balance.getSupplyLot()));
            accumulator.add(balance);
        }

        return byLotId.values().stream()
                .map(LotInventoryAccumulator::toSnapshot)
                .filter(snapshot -> snapshot.onHand().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private Set<Integer> resolveOwnerFarmIds(Long ownerId) {
        if (ownerId == null) {
            return Set.of();
        }
        return farmQueryPort.findFarmsByOwnerId(ownerId).stream()
                .map(org.example.QuanLyMuaVu.module.farm.entity.Farm::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
    }

    private DashboardInventoryAlertsResponse.AlertItem buildAlertItem(
            LotInventorySnapshot snapshot,
            String alertType,
            String severity,
            String reason,
            LocalDateTime lastMovementAt) {
        return DashboardInventoryAlertsResponse.AlertItem.builder()
                .supplyLotId(snapshot.lotId())
                .itemName(snapshot.itemName())
                .lotCode(snapshot.lotCode())
                .warehouseName(snapshot.warehouseName())
                .locationLabel(snapshot.locationLabel())
                .quantity(snapshot.onHand())
                .unit(snapshot.unit())
                .expiryDate(snapshot.expiryDate())
                .alertType(alertType)
                .severity(severity)
                .reason(reason)
                .lastMovementAt(lastMovementAt)
                .build();
    }

    private LocalDateTime resolveLastMovementAt(List<StockMovement> movements) {
        if (movements == null || movements.isEmpty()) {
            return null;
        }
        return movements.stream()
                .map(StockMovement::getMovementDate)
                .filter(date -> date != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean hasAbnormalMovement(
            List<StockMovement> movements,
            BigDecimal onHand,
            LocalDateTime now,
            int windowDays,
            int adjustCountThreshold,
            BigDecimal adjustRatioThreshold) {
        if (movements == null || movements.isEmpty()) {
            return false;
        }
        LocalDateTime thresholdDate = now.minusDays(Math.max(windowDays, 1));

        long recentMovementCount = movements.stream()
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .count();

        long recentAdjustCount = movements.stream()
                .filter(movement -> movement.getMovementType() == StockMovementType.ADJUST)
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .count();

        BigDecimal effectiveOnHand = onHand != null ? onHand.abs() : BigDecimal.ZERO;
        BigDecimal ratioThreshold = adjustRatioThreshold != null
                ? adjustRatioThreshold.max(BigDecimal.ZERO)
                : DEFAULT_ABNORMAL_ADJUST_RATIO;
        BigDecimal largeAdjustThreshold = effectiveOnHand.multiply(ratioThreshold);

        boolean hasLargeAdjust = movements.stream()
                .filter(movement -> movement.getMovementType() == StockMovementType.ADJUST)
                .filter(movement -> movement.getMovementDate() != null && !movement.getMovementDate().isBefore(thresholdDate))
                .map(StockMovement::getQuantity)
                .filter(quantity -> quantity != null)
                .map(BigDecimal::abs)
                .anyMatch(quantity -> quantity.compareTo(largeAdjustThreshold) >= 0);

        return recentAdjustCount >= Math.max(adjustCountThreshold, 1)
                || hasLargeAdjust
                || recentMovementCount >= 8;
    }

    private boolean isLowStock(BigDecimal onHand, BigDecimal threshold) {
        if (onHand == null) {
            return false;
        }
        BigDecimal effectiveThreshold = threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        return onHand.compareTo(BigDecimal.ZERO) > 0 && onHand.compareTo(effectiveThreshold) <= 0;
    }

    private String resolveLowStockSeverity(BigDecimal onHand, BigDecimal threshold) {
        if (onHand == null || threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return "MEDIUM";
        }
        BigDecimal criticalThreshold = threshold.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        return onHand.compareTo(criticalThreshold) <= 0 ? "HIGH" : "MEDIUM";
    }

    private int severityRank(String severity) {
        if (severity == null) {
            return 5;
        }
        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 1;
            case "HIGH" -> 2;
            case "MEDIUM" -> 3;
            case "LOW" -> 4;
            default -> 5;
        };
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_ALERT_LIMIT;
        }
        return Math.min(limit, MAX_ALERT_LIMIT);
    }

    private String buildLocationLabel(org.example.QuanLyMuaVu.module.inventory.entity.StockLocation location) {
        if (location == null) {
            return null;
        }
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

    private InventoryAlertSettings resolveInventoryAlertSettings() {
        AppProperties.Inventory inventoryCfg = appProperties.getInventory();
        AppProperties.InventoryAlerts alertsCfg = inventoryCfg != null ? inventoryCfg.getAlerts() : null;

        BigDecimal lowStockThreshold = alertsCfg != null ? alertsCfg.getLowStockThreshold() : null;
        boolean thresholdConfigured = lowStockThreshold != null && lowStockThreshold.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal effectiveLowStockThreshold = thresholdConfigured
                ? lowStockThreshold
                : DEFAULT_LOW_STOCK_THRESHOLD;

        Integer expiringSoonDays = alertsCfg != null ? alertsCfg.getExpiringSoonDays() : null;
        Integer noMovementDays = alertsCfg != null ? alertsCfg.getNoMovementDays() : null;
        Integer abnormalAdjustCount = alertsCfg != null ? alertsCfg.getAbnormalAdjustCount() : null;
        BigDecimal abnormalAdjustRatio = alertsCfg != null ? alertsCfg.getAbnormalAdjustRatio() : null;

        return new InventoryAlertSettings(
                effectiveLowStockThreshold,
                expiringSoonDays != null && expiringSoonDays > 0 ? expiringSoonDays : DEFAULT_EXPIRY_WINDOW_DAYS,
                noMovementDays != null && noMovementDays > 0 ? noMovementDays : DEFAULT_NO_MOVEMENT_DAYS,
                abnormalAdjustCount != null && abnormalAdjustCount > 0
                        ? abnormalAdjustCount
                        : DEFAULT_ABNORMAL_ADJUST_COUNT,
                abnormalAdjustRatio != null && abnormalAdjustRatio.compareTo(BigDecimal.ZERO) > 0
                        ? abnormalAdjustRatio
                        : DEFAULT_ABNORMAL_ADJUST_RATIO,
                thresholdConfigured ? THRESHOLD_SOURCE_BACKEND_CONFIG : THRESHOLD_SOURCE_BACKEND_DEFAULT,
                thresholdConfigured);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private record OwnerLotSnapshot(Integer lotId, String status, LocalDate expiryDate, BigDecimal onHand) {
    }

    private record LotInventorySnapshot(
            Integer lotId,
            String itemName,
            String lotCode,
            String warehouseName,
            String locationLabel,
            BigDecimal onHand,
            String unit,
            LocalDate expiryDate) {
    }

    private record InventoryAlertSettings(
            BigDecimal lowStockThreshold,
            int expiringSoonDays,
            int noMovementDays,
            int abnormalAdjustCount,
            BigDecimal abnormalAdjustRatio,
            String thresholdSource,
            boolean thresholdConfigured) {
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

    private final class LotInventoryAccumulator {
        private final SupplyLot lot;
        private BigDecimal onHand = BigDecimal.ZERO;
        private String warehouseName;
        private String locationLabel;

        private LotInventoryAccumulator(SupplyLot lot) {
            this.lot = lot;
        }

        private void add(InventoryBalance balance) {
            BigDecimal quantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
            onHand = onHand.add(quantity);

            String nextWarehouseName = balance.getWarehouse() != null ? balance.getWarehouse().getName() : null;
            if (warehouseName == null) {
                warehouseName = nextWarehouseName;
            } else if (nextWarehouseName != null && !warehouseName.equals(nextWarehouseName)) {
                warehouseName = "Multiple Warehouses";
            }

            String nextLocationLabel = buildLocationLabel(balance.getLocation());
            if (locationLabel == null) {
                locationLabel = nextLocationLabel;
            } else if (nextLocationLabel != null && !locationLabel.equals(nextLocationLabel)) {
                locationLabel = "Multiple Locations";
            }
        }

        private LotInventorySnapshot toSnapshot() {
            return new LotInventorySnapshot(
                    lot.getId(),
                    lot.getSupplyItem() != null ? lot.getSupplyItem().getName() : "Unknown Item",
                    lot.getBatchCode(),
                    warehouseName,
                    locationLabel,
                    onHand,
                    lot.getSupplyItem() != null ? lot.getSupplyItem().getUnit() : null,
                    lot.getExpiryDate());
        }
    }
}
