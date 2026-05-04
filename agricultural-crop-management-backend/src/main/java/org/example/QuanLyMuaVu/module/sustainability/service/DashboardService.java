package org.example.QuanLyMuaVu.module.sustainability.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.PlotStatusResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
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

    private final CurrentUserService currentUserService;
    private final FarmerOwnershipService ownershipService;
    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;
    private final IdentityQueryPort identityQueryPort;

    private final DashboardKpiService kpiService;
    private final DashboardAlertsService alertsService;
    private final DashboardTaskReadService dashboardTaskReadService;
    private final DashboardPlotStatusReadService dashboardPlotStatusReadService;

    public DashboardOverviewResponse getOverview(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        ensureUserExists(ownerId);

        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeasonContext(seasonId, ownerId);

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
                .build();
    }

    public Page<TodayTaskResponse> getTodayTasks(Integer seasonId, Pageable pageable) {
        return dashboardTaskReadService.getTodayTasks(seasonId, pageable);
    }

    public List<TodayTaskResponse> getUpcomingTasks(int days, Integer seasonId) {
        return dashboardTaskReadService.getUpcomingTasks(days, seasonId);
    }

    public List<PlotStatusResponse> getPlotStatus(Integer seasonId) {
        return dashboardPlotStatusReadService.getPlotStatus(seasonId);
    }

    public List<LowStockAlertResponse> getLowStock(int limit) {
        return alertsService.getLowStock(limit);
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
}
