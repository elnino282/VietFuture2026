package org.example.QuanLyMuaVu.module.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryHealthResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.example.QuanLyMuaVu.module.admin.service.AdminDashboardFacade;
import org.example.QuanLyMuaVu.module.admin.service.AdminInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Dashboard Controller
 * Provides system-wide statistics for admin dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardFacade adminDashboardFacade;
    private final AdminInventoryService adminInventoryService;

    /**
     * GET /api/v1/admin/dashboard-stats
     * Returns system-wide dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        log.info("Admin requesting dashboard stats");

        DashboardStatsDTO stats = adminDashboardFacade.getDashboardStats();

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }

    /**
     * GET /api/v1/admin/dashboard/inventory-health
     * Returns inventory health widget data.
     */
    @GetMapping("/dashboard/inventory-health")
    public ResponseEntity<ApiResponse<AdminInventoryHealthResponse>> getInventoryHealth(
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "includeExpiring", required = false) Boolean includeExpiring,
            @RequestParam(value = "limit", required = false) Integer limit) {
        log.info("Admin requesting inventory health, windowDays={}, includeExpiring={}, limit={}",
                windowDays, includeExpiring, limit);

        AdminInventoryHealthResponse response = adminInventoryService.getInventoryHealth(
                windowDays,
                includeExpiring,
                limit);

        return ResponseEntity.ok(ApiResponse.success("Inventory health retrieved", response));
    }
}
