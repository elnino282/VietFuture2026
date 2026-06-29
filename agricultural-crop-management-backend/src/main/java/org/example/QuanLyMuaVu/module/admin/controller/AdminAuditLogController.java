package org.example.QuanLyMuaVu.module.admin.controller;

import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminAuditLogResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Audit Log Controller (Deprecated - Moved to admin-reporting-service)
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Deprecated
public class AdminAuditLogController {

    @GetMapping
    public ApiResponse<PageResponse<AdminAuditLogResponse>> listAuditLogs(
            @RequestParam(value = "from", required = false) LocalDateTime from,
            @RequestParam(value = "to", required = false) LocalDateTime to,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "entityId", required = false) Integer entityId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        throw new UnsupportedOperationException("This endpoint has been migrated to the admin-reporting-service.");
    }
}
