package org.example.QuanLyMuaVu.module.admin.service;

import java.time.LocalDate;
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
                .filter(p -> (p.getIncidentCount() != null && p.getIncidentCount() > 0)
                        || (p.getOverdueTaskCount() != null && p.getOverdueTaskCount() > 0))
                .toList();
    }

    public List<DashboardStatsDTO.InventoryHealth> getInventoryHealth(int windowDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(windowDays);
        return adminDashboardReadRepository.findInventoryHealth(today, cutoff);
    }
}
