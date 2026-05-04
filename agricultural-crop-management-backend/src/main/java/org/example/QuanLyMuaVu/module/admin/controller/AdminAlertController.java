package org.example.QuanLyMuaVu.module.admin.controller;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminAlertRefreshRequest;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminAlertSendRequest;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminAlertStatusUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminAlertResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminAlertService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/alerts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertController {

    AdminAlertService adminAlertService;

    @GetMapping
    public ApiResponse<PageResponse<AdminAlertResponse>> listAlerts(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {

        return ApiResponse.success(adminAlertService.listAlerts(
                type,
                severity,
                status,
                farmId,
                windowDays,
                page,
                limit));
    }

    @PostMapping("/refresh")
    public ApiResponse<List<AdminAlertResponse>> refreshAlerts(@RequestBody(required = false) AdminAlertRefreshRequest request) {
        Integer windowDays = request != null ? request.getWindowDays() : null;
        return ApiResponse.success(adminAlertService.refreshAlerts(windowDays));
    }

    @PostMapping("/{id}/send")
    public ApiResponse<AdminAlertResponse> sendAlert(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) AdminAlertSendRequest request) {
        return ApiResponse.success(adminAlertService.sendAlert(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminAlertResponse> updateStatus(
            @PathVariable("id") Integer id,
            @RequestBody AdminAlertStatusUpdateRequest request) {
        return ApiResponse.success(adminAlertService.updateStatus(id, request != null ? request.getStatus() : null));
    }
}
