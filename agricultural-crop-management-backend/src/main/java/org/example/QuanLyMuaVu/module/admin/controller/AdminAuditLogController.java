package org.example.QuanLyMuaVu.module.admin.controller;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminAuditLogResponse;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private static final int MAX_PAGE_SIZE = 200;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<PageResponse<AdminAuditLogResponse>> listAuditLogs(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "entityId", required = false) Integer entityId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {

        int safePage = Math.max(page != null ? page : 0, 0);
        int safeSize = Math.max(Math.min(size != null ? size : 20, MAX_PAGE_SIZE), 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "performedAt"));

        return ApiResponse.success(
                auditLogService.listAuditLogs(
                        from,
                        to,
                        module,
                        entityType,
                        action,
                        user,
                        entityId,
                        pageable));
    }
}
