package org.example.QuanLyMuaVu.module.admin.controller;

import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryLotDetailResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryMovementResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryOptionsResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryRiskLotResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminInventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    AdminInventoryService adminInventoryService;

    @GetMapping("/options")
    public ApiResponse<AdminInventoryOptionsResponse> getOptions() {
        return ApiResponse.success(adminInventoryService.getOptions());
    }

    @GetMapping("/lots")
    public ApiResponse<PageResponse<AdminInventoryRiskLotResponse>> listRiskLots(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "lowStockThreshold", required = false) BigDecimal lowStockThreshold,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {

        return ApiResponse.success(adminInventoryService.listRiskLots(
                farmId,
                itemId,
                status,
                severity,
                windowDays,
                q,
                sort,
                lowStockThreshold,
                page,
                limit));
    }

    @GetMapping("/lots/{lotId}")
    public ApiResponse<AdminInventoryLotDetailResponse> getLotDetail(@PathVariable("lotId") Integer lotId) {
        return ApiResponse.success(adminInventoryService.getLotDetail(lotId));
    }

    @GetMapping("/lots/{lotId}/movements")
    public ApiResponse<List<AdminInventoryMovementResponse>> getLotMovements(@PathVariable("lotId") Integer lotId) {
        return ApiResponse.success(adminInventoryService.getLotMovements(lotId));
    }
}
