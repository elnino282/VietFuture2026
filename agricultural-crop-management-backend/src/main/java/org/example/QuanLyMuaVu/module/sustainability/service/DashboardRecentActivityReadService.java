package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.repository.IncidentRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderRepository;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.entity.TaskProgressLog;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskProgressLogRepository;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardRecentActivityResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardRecentActivityReadService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final TaskProgressLogRepository taskProgressLogRepository;
    private final FieldLogRepository fieldLogRepository;
    private final IncidentRepository incidentRepository;
    private final HarvestRepository harvestRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MarketplaceOrderRepository marketplaceOrderRepository;

    public List<DashboardRecentActivityResponse> getRecentActivities(Long ownerId, int requestedLimit) {
        int limit = sanitizeLimit(requestedLimit);
        Pageable pageable = PageRequest.of(0, limit);

        List<DashboardRecentActivityResponse> merged = new ArrayList<>();
        merged.addAll(taskProgressLogRepository.findRecentByOwnerId(ownerId, pageable).stream()
                .map(this::toTaskUpdateActivity)
                .toList());
        merged.addAll(fieldLogRepository.findRecentByOwnerId(ownerId, pageable).stream()
                .map(this::toFieldLogActivity)
                .toList());
        merged.addAll(incidentRepository.findRecentByOwnerId(ownerId, pageable).stream()
                .map(this::toIncidentActivity)
                .toList());
        merged.addAll(harvestRepository.findRecentByOwnerId(ownerId, pageable).stream()
                .map(this::toHarvestActivity)
                .toList());
        merged.addAll(stockMovementRepository.findRecentByOwnerId(ownerId, pageable).stream()
                .map(this::toStockMovementActivity)
                .toList());
        try {
            merged.addAll(marketplaceOrderRepository.findRecentByFarmerUserId(ownerId, pageable).stream()
                    .map(this::toMarketplaceOrderActivity)
                    .toList());
        } catch (RuntimeException ex) {
            log.warn("Skipping marketplace recent activity due to read error for ownerId={}", ownerId, ex);
        }

        return merged.stream()
                .filter(activity -> activity.getOccurredAt() != null)
                .sorted(Comparator.comparing(DashboardRecentActivityResponse::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private DashboardRecentActivityResponse toTaskUpdateActivity(TaskProgressLog log) {
        Integer seasonId = log.getTask() != null && log.getTask().getSeason() != null
                ? log.getTask().getSeason().getId()
                : null;
        String taskTitle = log.getTask() != null && log.getTask().getTitle() != null
                ? log.getTask().getTitle()
                : localize("Task update", "Cập nhật công việc");
        String plotName = log.getTask() != null
                && log.getTask().getSeason() != null
                && log.getTask().getSeason().getPlot() != null
                ? log.getTask().getSeason().getPlot().getPlotName()
                : null;

        StringBuilder description = new StringBuilder(
                localize("Progress updated to ", "Tiến độ cập nhật lên ")
                        + log.getProgressPercent()
                        + "%"
        );
        if (plotName != null && !plotName.isBlank()) {
            description.append(localize(" on ", " tại ")).append(plotName);
        }
        if (log.getNote() != null && !log.getNote().isBlank()) {
            description.append(". ").append(log.getNote());
        }

        return DashboardRecentActivityResponse.builder()
                .id("task-progress-" + log.getId())
                .type("TASK_UPDATE")
                .title(taskTitle)
                .description(description.toString())
                .occurredAt(log.getLoggedAt())
                .actorName(resolveActorName(log.getEmployee()))
                .entityType("TASK")
                .entityId(log.getTask() != null && log.getTask().getId() != null
                        ? String.valueOf(log.getTask().getId())
                        : String.valueOf(log.getId()))
                .actionUrl(seasonId != null
                        ? "/farmer/seasons/" + seasonId + "/workspace/tasks"
                        : "/farmer/dashboard")
                .build();
    }

    private DashboardRecentActivityResponse toFieldLogActivity(FieldLog log) {
        Integer seasonId = log.getSeason() != null ? log.getSeason().getId() : null;
        LocalDateTime occurredAt = log.getCreatedAt() != null
                ? log.getCreatedAt()
                : toStartOfDay(log.getLogDate());

        return DashboardRecentActivityResponse.builder()
                .id("field-log-" + log.getId())
                .type("FIELD_LOG")
                .title(localize("Field log: ", "Nhật ký đồng ruộng: ") + formatLabel(log.getLogType()))
                .description((log.getNotes() == null || log.getNotes().isBlank())
                        ? localize("Field observation recorded.", "Đã ghi nhận quan sát đồng ruộng.")
                        : log.getNotes())
                .occurredAt(occurredAt)
                .actorName(null)
                .entityType("FIELD_LOG")
                .entityId(log.getId() != null ? String.valueOf(log.getId()) : "")
                .actionUrl(seasonId != null
                        ? "/farmer/seasons/" + seasonId + "/workspace/field-logs"
                        : "/farmer/dashboard")
                .build();
    }

    private DashboardRecentActivityResponse toIncidentActivity(Incident incident) {
        Integer seasonId = incident.getSeasonId();
        String incidentType = incident.getIncidentType() == null || incident.getIncidentType().isBlank()
                ? localize("Incident", "Sự cố")
                : formatLabel(incident.getIncidentType());
        String severity = incident.getSeverity() != null
                ? incident.getSeverity().name()
                : localize("UNKNOWN", "KHÔNG RÕ");
        String status = incident.getStatus() != null
                ? incident.getStatus().name()
                : localize("UNKNOWN", "KHÔNG RÕ");
        String description = incident.getDescription() != null && !incident.getDescription().isBlank()
                ? incident.getDescription()
                : localize("Severity: ", "Mức độ: ")
                    + formatLabel(severity)
                    + localize(", status: ", ", trạng thái: ")
                    + formatLabel(status);

        return DashboardRecentActivityResponse.builder()
                .id("incident-" + incident.getId())
                .type("INCIDENT")
                .title(localize("Incident reported: ", "Đã ghi nhận sự cố: ") + incidentType)
                .description(description)
                .occurredAt(incident.getCreatedAt())
                .actorName(resolveActorName(incident.getReportedBy()))
                .entityType("INCIDENT")
                .entityId(incident.getId() != null ? String.valueOf(incident.getId()) : "")
                .actionUrl(seasonId != null
                        ? "/farmer/seasons/" + seasonId + "/workspace/disease"
                        : "/farmer/incidents")
                .build();
    }

    private DashboardRecentActivityResponse toHarvestActivity(Harvest harvest) {
        Integer seasonId = harvest.getSeason() != null ? harvest.getSeason().getId() : null;
        LocalDateTime occurredAt = harvest.getCreatedAt() != null
                ? harvest.getCreatedAt()
                : toStartOfDay(harvest.getHarvestDate());

        String quantity = harvest.getQuantity() != null
                ? harvest.getQuantity().stripTrailingZeros().toPlainString()
                : "0";

        return DashboardRecentActivityResponse.builder()
                .id("harvest-" + harvest.getId())
                .type("HARVEST")
                .title(localize("Harvest recorded", "Đã ghi nhận thu hoạch"))
                .description(localize("Harvest quantity: ", "Sản lượng thu hoạch: ") + quantity)
                .occurredAt(occurredAt)
                .actorName(null)
                .entityType("HARVEST")
                .entityId(harvest.getId() != null ? String.valueOf(harvest.getId()) : "")
                .actionUrl(seasonId != null
                        ? "/farmer/seasons/" + seasonId + "/workspace/harvest"
                        : "/farmer/dashboard")
                .build();
    }

    private DashboardRecentActivityResponse toStockMovementActivity(StockMovement movement) {
        String itemName = movement.getSupplyLot() != null
                && movement.getSupplyLot().getSupplyItem() != null
                && movement.getSupplyLot().getSupplyItem().getName() != null
                        ? movement.getSupplyLot().getSupplyItem().getName()
                        : localize("Inventory item", "Vật tư kho");
        String direction = movement.getMovementType() != null
                ? movement.getMovementType().name()
                : localize("UNKNOWN", "KHÔNG RÕ");
        String quantity = formatDecimal(movement.getQuantity());

        return DashboardRecentActivityResponse.builder()
                .id("stock-movement-" + movement.getId())
                .type("WAREHOUSE_MOVEMENT")
                .title(localize("Warehouse movement: ", "Luân chuyển kho: ") + formatLabel(direction))
                .description(itemName + localize(" quantity ", " | Số lượng ") + quantity)
                .occurredAt(movement.getMovementDate())
                .actorName(null)
                .entityType("STOCK_MOVEMENT")
                .entityId(movement.getId() != null ? String.valueOf(movement.getId()) : "")
                .actionUrl("/farmer/inventory")
                .build();
    }

    private DashboardRecentActivityResponse toMarketplaceOrderActivity(MarketplaceOrder order) {
        LocalDateTime occurredAt = order.getUpdatedAt() != null
                ? order.getUpdatedAt()
                : order.getCreatedAt();
        String status = order.getStatus() != null ? order.getStatus().name() : localize("UNKNOWN", "KHÔNG RÕ");
        String orderCode = order.getOrderCode() != null ? order.getOrderCode() : "#" + order.getId();
        String totalAmount = formatDecimal(order.getTotalAmount());

        return DashboardRecentActivityResponse.builder()
                .id("marketplace-order-" + order.getId())
                .type("MARKETPLACE_ORDER")
                .title(localize("Order ", "Đơn ") + orderCode + " - " + formatLabel(status))
                .description(localize("Total amount: ", "Tổng tiền: ") + totalAmount)
                .occurredAt(occurredAt)
                .actorName(null)
                .entityType("MARKETPLACE_ORDER")
                .entityId(order.getId() != null ? String.valueOf(order.getId()) : "")
                .actionUrl(order.getId() != null ? "/farmer/marketplace-orders/" + order.getId() : "/farmer/marketplace-orders")
                .build();
    }

    private int sanitizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private String resolveActorName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return null;
    }

    private String formatLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return localize("Unknown", "Không rõ");
        }
        String normalized = raw.trim().replace('-', '_');
        String[] tokens = normalized.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(token.substring(0, 1).toUpperCase(Locale.ROOT));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.length() == 0 ? localize("Unknown", "Không rõ") : builder.toString();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private boolean isVietnameseLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    private String localize(String english, String vietnamese) {
        return isVietnameseLocale() ? vietnamese : english;
    }
}
