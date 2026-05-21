package org.example.QuanLyMuaVu.module.sustainability.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.inventory.dto.response.DashboardInventoryAlertsResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardDataCompletenessWarningResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardIncidentAlertResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardRecentActivityResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.PlotStatusResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard orchestration service.
 *
 * Responsibilities:
 * - Resolve season context + top-level overview payload.
 * - Delegate task reads and plot status reads to dedicated read services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private static final String DATA_WARNING_SOURCE = "SUSTAINABILITY_OVERVIEW";
    private static final String DATA_WARNING_TYPE = "DATA_COMPLETENESS";
    private static final String DATA_WARNING_STATUS = "ACTION_REQUIRED";

    private final CurrentUserService currentUserService;
    private final FarmerOwnershipService ownershipService;
    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;
    private final IdentityQueryPort identityQueryPort;

    private final DashboardKpiService kpiService;
    private final DashboardAlertsService alertsService;
    private final DashboardTaskReadService dashboardTaskReadService;
    private final DashboardPlotStatusReadService dashboardPlotStatusReadService;
    private final DashboardRecentActivityReadService dashboardRecentActivityReadService;
    private final SustainabilityDashboardService sustainabilityDashboardService;

    public DashboardOverviewResponse getOverview(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        ensureUserExists(ownerId);

        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeasonContext(seasonId, ownerId);
        DashboardMeta metadata = resolveDashboardMeta(seasonId, season);

        return DashboardOverviewResponse.builder()
                .seasonContext(buildSeasonContext(season))
                .counts(buildCounts(ownerId))
                .kpis(kpiService.buildKpis(season))
                .expenses(kpiService.buildExpenses(season))
                .harvest(kpiService.buildHarvest(season))
                .alerts(alertsService.buildAlerts(ownerId))
                .taskStatus(kpiService.buildTaskStatusSummary(season))
                .inventoryRisk(alertsService.buildInventoryRisk(ownerId))
                .lotStatus(alertsService.buildLotStatus(ownerId))
                .sustainabilityAlerts(alertsService.buildSustainabilityAlerts(season != null ? season.getId() : null))
                .unavailableReasons(metadata.unavailableReasons())
                .missingInputs(metadata.missingInputs())
                .build();
    }

    public Page<TodayTaskResponse> getTodayTasks(Integer seasonId, Pageable pageable) {
        return dashboardTaskReadService.getTodayTasks(seasonId, pageable);
    }

    public List<TodayTaskResponse> getUpcomingTasks(int days, Integer seasonId) {
        return dashboardTaskReadService.getUpcomingTasks(days, seasonId);
    }

    public List<DashboardDataCompletenessWarningResponse> getDataCompletenessWarnings(Integer seasonId) {
        SustainabilityOverviewResponse overview = sustainabilityDashboardService.getOverview(
                "field",
                seasonId,
                null,
                null
        );
        if (overview == null || overview.getMissingInputs() == null || overview.getMissingInputs().isEmpty()) {
            return List.of();
        }
        Integer resolvedSeasonId = overview.getSeasonId() != null ? overview.getSeasonId() : seasonId;
        return overview.getMissingInputs().stream()
                .filter(inputCode -> inputCode != null && !inputCode.isBlank())
                .distinct()
                .map(inputCode -> toDataCompletenessWarning(inputCode, resolvedSeasonId))
                .toList();
    }

    public List<PlotStatusResponse> getPlotStatus(Integer seasonId) {
        return dashboardPlotStatusReadService.getPlotStatus(seasonId);
    }

    public List<LowStockAlertResponse> getLowStock(int limit) {
        return alertsService.getLowStock(limit);
    }

    public DashboardInventoryAlertsResponse getInventoryAlerts(int limit) {
        return alertsService.getInventoryAlerts(limit);
    }

    public List<DashboardIncidentAlertResponse> getIncidentAlerts(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        ensureUserExists(ownerId);
        if (seasonId != null) {
            ownershipService.requireOwnedSeason(seasonId);
        }
        return alertsService.getIncidentAlerts(seasonId);
    }

    public List<DashboardRecentActivityResponse> getRecentActivities(int limit) {
        Long ownerId = currentUserService.getCurrentUserId();
        ensureUserExists(ownerId);
        return dashboardRecentActivityReadService.getRecentActivities(ownerId, limit);
    }

    private void ensureUserExists(Long ownerId) {
        org.example.QuanLyMuaVu.module.identity.entity.User user = identityQueryPort.findUserById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getId() == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private org.example.QuanLyMuaVu.module.season.entity.Season resolveSeasonContext(Integer seasonId, Long ownerId) {
        if (seasonId != null) {
            return ownershipService.requireOwnedSeason(seasonId);
        }

        List<org.example.QuanLyMuaVu.module.season.entity.Season> activeSeasons = seasonQueryPort.findActiveSeasonsByOwnerIdOrderByStartDateDesc(ownerId);
        if (!activeSeasons.isEmpty()) {
            return activeSeasons.get(0);
        }

        List<org.example.QuanLyMuaVu.module.season.entity.Season> allSeasons = seasonQueryPort.findAllSeasonsByOwnerId(ownerId);
        if (!allSeasons.isEmpty()) {
            return allSeasons.stream()
                    .max(Comparator.comparing(org.example.QuanLyMuaVu.module.season.entity.Season::getStartDate))
                    .orElse(null);
        }

        return null;
    }

    private DashboardOverviewResponse.SeasonContext buildSeasonContext(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null) {
            return null;
        }
        return DashboardOverviewResponse.SeasonContext.builder()
                .seasonId(season.getId())
                .seasonName(season.getSeasonName())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .plannedHarvestDate(season.getPlannedHarvestDate())
                .build();
    }

    private DashboardOverviewResponse.Counts buildCounts(Long ownerId) {
        long activeFarms = farmQueryPort.countActiveFarmsByOwnerId(ownerId);
        long activePlots = farmQueryPort.countPlotsByOwnerId(ownerId);

        Map<String, Integer> seasonsByStatus = new LinkedHashMap<>();
        for (SeasonStatus status : SeasonStatus.values()) {
            long count = seasonQueryPort.countSeasonsByStatusAndOwnerId(status, ownerId);
            seasonsByStatus.put(status.name(), (int) count);
        }

        return DashboardOverviewResponse.Counts.builder()
                .activeFarms((int) activeFarms)
                .activePlots((int) activePlots)
                .seasonsByStatus(seasonsByStatus)
                .build();
    }

    private DashboardDataCompletenessWarningResponse toDataCompletenessWarning(
            String inputCode,
            Integer seasonId
    ) {
        String normalized = inputCode == null
                ? "UNKNOWN"
                : inputCode.trim().toUpperCase(Locale.ROOT);
        boolean vietnamese = isVietnameseLocale();
        return DashboardDataCompletenessWarningResponse.builder()
                .warningId("missing-input-" + normalized.toLowerCase(Locale.ROOT))
                .title(localize(vietnamese, "Missing required input: ", "Thiếu dữ liệu bắt buộc: ")
                        + formatInputLabel(normalized, vietnamese))
                .source(DATA_WARNING_SOURCE)
                .type(DATA_WARNING_TYPE)
                .status(DATA_WARNING_STATUS)
                .dueDate(LocalDate.now())
                .actionTarget(resolveActionTarget(normalized, seasonId))
                .seasonId(seasonId)
                .inputCode(normalized)
                .build();
    }

    private String resolveActionTarget(String inputCode, Integer seasonId) {
        if (seasonId == null) {
            return "/farmer/dashboard";
        }
        String workspacePath;
        if ("IRRIGATION_WATER".equals(inputCode)) {
            workspacePath = "irrigation-water-analyses";
        } else if ("SOIL_LEGACY".equals(inputCode)) {
            workspacePath = "soil-tests";
        } else {
            workspacePath = "nutrient-inputs";
        }
        return "/farmer/seasons/" + seasonId + "/workspace/" + workspacePath;
    }

    private String formatInputLabel(String inputCode, boolean vietnamese) {
        if (vietnamese) {
            return switch (inputCode) {
                case "MINERAL_FERTILIZER" -> "Phân bón vô cơ";
                case "ORGANIC_FERTILIZER" -> "Phân bón hữu cơ";
                case "BIOLOGICAL_FIXATION" -> "Cố định đạm sinh học";
                case "IRRIGATION_WATER" -> "Nước tưới";
                case "ATMOSPHERIC_DEPOSITION" -> "Lắng đọng khí quyển";
                case "SEED_IMPORT" -> "Đạm từ hạt giống";
                case "SOIL_LEGACY" -> "Đạm tồn dư trong đất";
                case "CONTROL_SUPPLY" -> "Nguồn đạm kiểm soát";
                default -> formatInputLabelDefault(inputCode);
            };
        }
        return formatInputLabelDefault(inputCode);
    }

    private String formatInputLabelDefault(String inputCode) {
        return Arrays.stream(inputCode.toLowerCase(Locale.ROOT).split("_"))
                .filter(token -> !token.isBlank())
                .map(token -> token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(inputCode);
    }

    private DashboardMeta resolveDashboardMeta(
            Integer requestedSeasonId,
            org.example.QuanLyMuaVu.module.season.entity.Season season
    ) {
        LinkedHashSet<String> unavailableReasons = new LinkedHashSet<>();
        if (season == null) {
            unavailableReasons.add(SustainabilityDashboardService.REASON_NO_ACTIVE_SEASON);
        }

        List<String> missingInputs = List.of();
        try {
            SustainabilityOverviewResponse overview = sustainabilityDashboardService.getOverview(
                    "field",
                    requestedSeasonId,
                    null,
                    null
            );
            if (overview != null) {
                if (overview.getMissingInputs() != null) {
                    missingInputs = overview.getMissingInputs();
                }
                if (overview.getUnavailableReasons() != null) {
                    unavailableReasons.addAll(overview.getUnavailableReasons());
                }
            }
        } catch (AppException ex) {
            if (ex.getErrorCode() != ErrorCode.PLOT_NOT_FOUND) {
                throw ex;
            }
        }

        return new DashboardMeta(List.copyOf(unavailableReasons), missingInputs);
    }

    private record DashboardMeta(
            List<String> unavailableReasons,
            List<String> missingInputs
    ) {
    }

    private boolean isVietnameseLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    private String localize(boolean vietnameseLocale, String english, String vietnamese) {
        return vietnameseLocale ? vietnamese : english;
    }
}
