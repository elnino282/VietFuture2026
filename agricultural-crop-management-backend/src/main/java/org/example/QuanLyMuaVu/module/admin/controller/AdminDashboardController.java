package org.example.QuanLyMuaVu.module.admin.controller;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryHealthResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminPendingApprovalItemDTO;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Dashboard Controller (Deprecated - Moved to admin-reporting-service)
 */
@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Deprecated
public class AdminDashboardController {

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        throw new UnsupportedOperationException("This endpoint has been migrated to the admin-reporting-service.");
    }

    @GetMapping("/dashboard/pending-approvals")
    public ResponseEntity<ApiResponse<List<AdminPendingApprovalItemDTO>>> getPendingApprovals(
            @RequestParam(value = "limit", required = false) Integer limit) {
        throw new UnsupportedOperationException("This endpoint has been migrated to the admin-reporting-service.");
    }

    @GetMapping("/dashboard/inventory-health")
    public ResponseEntity<ApiResponse<AdminInventoryHealthResponse>> getInventoryHealth(
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "includeExpiring", required = false) Boolean includeExpiring,
            @RequestParam(value = "limit", required = false) Integer limit) {
        throw new UnsupportedOperationException("This endpoint has been migrated to the admin-reporting-service.");
    }
}
