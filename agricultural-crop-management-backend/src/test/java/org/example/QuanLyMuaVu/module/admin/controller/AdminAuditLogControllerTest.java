package org.example.QuanLyMuaVu.module.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminAuditLogResponse;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminAuditLogController.class)
@Import(AdminAuditLogControllerTest.MethodSecurityTestConfig.class)
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAuditLogs_withAdminRole_returnsPagedLogsAndAppliesFilters() throws Exception {
        AdminAuditLogResponse row = AdminAuditLogResponse.builder()
                .id(1L)
                .module("IDENTITY")
                .operation("RBAC_STATUS_UPDATED")
                .entityType("IDENTITY_USER")
                .entityId(12)
                .performedBy("admin-user")
                .performedAt(LocalDateTime.parse("2026-04-26T10:15:30"))
                .reason("Admin updated user status")
                .build();

        PageResponse<AdminAuditLogResponse> response = new PageResponse<>();
        response.setItems(List.of(row));
        response.setPage(0);
        response.setSize(20);
        response.setTotalElements(1);
        response.setTotalPages(1);

        when(auditLogService.listAuditLogs(
                eq(LocalDateTime.parse("2026-04-20T00:00:00")),
                eq(LocalDateTime.parse("2026-04-26T23:59:59")),
                eq("IDENTITY"),
                eq("IDENTITY_USER"),
                eq("RBAC_STATUS_UPDATED"),
                eq("admin-user"),
                eq(12),
                any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .param("from", "2026-04-20T00:00:00")
                .param("to", "2026-04-26T23:59:59")
                .param("module", "IDENTITY")
                .param("entityType", "IDENTITY_USER")
                .param("action", "RBAC_STATUS_UPDATED")
                .param("user", "admin-user")
                .param("entityId", "12")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].module").value("IDENTITY"))
                .andExpect(jsonPath("$.result.items[0].entityType").value("IDENTITY_USER"))
                .andExpect(jsonPath("$.result.totalElements").value(1));

        verify(auditLogService).listAuditLogs(
                eq(LocalDateTime.parse("2026-04-20T00:00:00")),
                eq(LocalDateTime.parse("2026-04-26T23:59:59")),
                eq("IDENTITY"),
                eq("IDENTITY_USER"),
                eq("RBAC_STATUS_UPDATED"),
                eq("admin-user"),
                eq(12),
                any());
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void listAuditLogs_withNonAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAuditLogs_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }
}
