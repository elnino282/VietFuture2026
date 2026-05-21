package org.example.QuanLyMuaVu.module.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.module.admin.repository.AdminDashboardReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardReadService {

    private final AdminDashboardReadRepository adminDashboardReadRepository;

    public List<DashboardStatsDTO.UserRoleCount> getUserRoleCounts() {
        return adminDashboardReadRepository.countUsersByRole();
    }

    public List<DashboardStatsDTO.UserStatusCount> getUserStatusCounts() {
        return adminDashboardReadRepository.countUsersByStatus();
    }

    public List<DashboardStatsDTO.SeasonStatusCount> getSeasonStatusCounts() {
        return adminDashboardReadRepository.countSeasonsByStatus();
    }

    public List<DashboardStatsDTO.RiskySeason> getRiskySeasons(int limit) {
        return adminDashboardReadRepository.findRiskySeasons(TaskStatus.OVERDUE, limit).stream()
                .map(this::enrichRiskBasis)
                .filter(p -> !p.getRiskBasis().isEmpty())
                .toList();
    }

    public DashboardStatsDTO.DataCoverage getRiskDataCoverage() {
        return DashboardStatsDTO.DataCoverage.builder()
                .incidentDataAvailable(adminDashboardReadRepository.hasIncidentSeasonData())
                .taskDataAvailable(adminDashboardReadRepository.hasTaskSeasonData())
                .build();
    }

    public List<DashboardStatsDTO.InventoryHealth> getInventoryHealth(int windowDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(windowDays);
        return adminDashboardReadRepository.findInventoryHealth(today, cutoff);
    }

    private DashboardStatsDTO.RiskySeason enrichRiskBasis(DashboardStatsDTO.RiskySeason riskySeason) {
        List<DashboardStatsDTO.RiskBasis> riskBasis = new ArrayList<>();

        if (isPositive(riskySeason.getIncidentCount())) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.OPEN_INCIDENTS);
        }
        if (isPositive(riskySeason.getOverdueTaskCount())) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.OVERDUE_TASKS);
        }
        if (isPositive(riskySeason.getHighFdnRiskCount())) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.HIGH_FDN_RISK);
        }
        if (isPositive(riskySeason.getInventoryRiskCount())) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.INVENTORY_RISK);
        }

        riskySeason.setRiskBasis(riskBasis);
        return riskySeason;
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0;
    }
}
