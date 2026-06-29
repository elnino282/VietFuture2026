package org.example.adminreporting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.response.AdminInventoryHealthResponse;
import org.example.adminreporting.dto.response.AdminPendingApprovalItemDTO;
import org.example.adminreporting.dto.response.DashboardStatsDTO;
import org.example.adminreporting.service.AdminDashboardService;
import org.example.adminreporting.service.AdminInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminInventoryService adminInventoryService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        log.info("Admin requesting dashboard stats in reporting service");
        DashboardStatsDTO stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }

    @GetMapping("/dashboard/pending-approvals")
    public ResponseEntity<ApiResponse<List<AdminPendingApprovalItemDTO>>> getPendingApprovals(
            @RequestParam(value = "limit", required = false) Integer limit) {
        log.info("Admin requesting pending approvals, limit={}", limit);
        List<AdminPendingApprovalItemDTO> approvals = adminDashboardService.getPendingApprovals(limit);
        return ResponseEntity.ok(ApiResponse.success("Pending approvals retrieved", approvals));
    }

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
