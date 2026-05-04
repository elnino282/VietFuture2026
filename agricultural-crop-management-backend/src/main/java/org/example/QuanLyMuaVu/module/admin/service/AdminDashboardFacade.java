package org.example.QuanLyMuaVu.module.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin dashboard orchestration facade.
 *
 * Responsibilities:
 * - Build top-level summary counts.
 * - Delegate read-heavy widgets to AdminDashboardReadService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardFacade {

    private final IdentityQueryPort identityQueryPort;
    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;
    private final AdminDashboardReadService adminDashboardReadService;

    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching admin dashboard stats");

        DashboardStatsDTO.Summary summary = DashboardStatsDTO.Summary.builder()
                .totalUsers(identityQueryPort.countUsers())
                .totalFarms(farmQueryPort.countFarms())
                .totalPlots(farmQueryPort.countPlots())
                .totalSeasons(seasonQueryPort.countSeasons())
                .build();

        return DashboardStatsDTO.builder()
                .summary(summary)
                .userRoleCounts(adminDashboardReadService.getUserRoleCounts())
                .userStatusCounts(adminDashboardReadService.getUserStatusCounts())
                .seasonStatusCounts(adminDashboardReadService.getSeasonStatusCounts())
                .riskySeasons(adminDashboardReadService.getRiskySeasons(10))
                .inventoryHealth(adminDashboardReadService.getInventoryHealth(30))
                .build();
    }
}
