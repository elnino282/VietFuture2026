package org.example.QuanLyMuaVu.module.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.module.admin.repository.AdminDashboardReadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardReadServiceTest {

    @Mock
    private AdminDashboardReadRepository adminDashboardReadRepository;

    @InjectMocks
    private AdminDashboardReadService adminDashboardReadService;

    @Test
    @DisplayName("getRiskySeasons maps risk basis and filters non-risk seasons")
    void getRiskySeasons_MapsRiskBasisAndFiltersNonRiskRows() {
        DashboardStatsDTO.RiskySeason seasonWithRisk = DashboardStatsDTO.RiskySeason.builder()
                .seasonId(11)
                .seasonName("Soybean Summer 2026")
                .incidentCount(2L)
                .overdueTaskCount(1L)
                .highFdnRiskCount(1L)
                .inventoryRiskCount(0L)
                .riskScore(4L)
                .build();
        DashboardStatsDTO.RiskySeason seasonWithoutRisk = DashboardStatsDTO.RiskySeason.builder()
                .seasonId(12)
                .seasonName("Rice Winter 2026")
                .incidentCount(0L)
                .overdueTaskCount(0L)
                .highFdnRiskCount(0L)
                .inventoryRiskCount(0L)
                .riskScore(0L)
                .build();

        when(adminDashboardReadRepository.findRiskySeasons(TaskStatus.OVERDUE, 10))
                .thenReturn(List.of(seasonWithRisk, seasonWithoutRisk));

        List<DashboardStatsDTO.RiskySeason> riskySeasons = adminDashboardReadService.getRiskySeasons(10);

        assertEquals(1, riskySeasons.size());
        DashboardStatsDTO.RiskySeason riskySeason = riskySeasons.get(0);
        assertEquals(11, riskySeason.getSeasonId());
        assertEquals(
                List.of(
                        DashboardStatsDTO.RiskBasis.OPEN_INCIDENTS,
                        DashboardStatsDTO.RiskBasis.OVERDUE_TASKS,
                        DashboardStatsDTO.RiskBasis.HIGH_FDN_RISK),
                riskySeason.getRiskBasis());
        assertFalse(riskySeason.getRiskBasis().contains(DashboardStatsDTO.RiskBasis.INVENTORY_RISK));
    }

    @Test
    @DisplayName("getRiskDataCoverage returns incident/task availability flags")
    void getRiskDataCoverage_ReturnsAvailabilityFlags() {
        when(adminDashboardReadRepository.hasIncidentSeasonData()).thenReturn(true);
        when(adminDashboardReadRepository.hasTaskSeasonData()).thenReturn(false);

        DashboardStatsDTO.DataCoverage coverage = adminDashboardReadService.getRiskDataCoverage();

        assertTrue(coverage.isIncidentDataAvailable());
        assertFalse(coverage.isTaskDataAvailable());
    }
}
