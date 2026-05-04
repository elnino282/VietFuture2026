package org.example.QuanLyMuaVu.module.admin.controller;

import java.util.List;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryRiskLotResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminInventoryController.class)
@Import(AdminInventoryControllerTest.MethodSecurityTestConfig.class)
class AdminInventoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminInventoryService adminInventoryService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRiskLots_withAdminRole_supportsSeverityStatusFarmItemFilters() throws Exception {
        AdminInventoryRiskLotResponse row = AdminInventoryRiskLotResponse.builder()
                .lotId(1001)
                .itemId(101)
                .itemName("Urea")
                .farmId(1)
                .farmName("Farm A")
                .status("ABNORMAL_MOVEMENT")
                .severity("HIGH")
                .build();

        PageResponse<AdminInventoryRiskLotResponse> pageResponse = new PageResponse<>();
        pageResponse.setItems(List.of(row));
        pageResponse.setPage(0);
        pageResponse.setSize(20);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);

        when(adminInventoryService.listRiskLots(
                eq(1),
                eq(101),
                eq("ABNORMAL_MOVEMENT"),
                eq("HIGH"),
                eq(30),
                eq("urea"),
                eq("EXPIRY_ASC"),
                eq(new java.math.BigDecimal("5")),
                eq(0),
                eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/inventory/lots")
                .param("farmId", "1")
                .param("itemId", "101")
                .param("status", "ABNORMAL_MOVEMENT")
                .param("severity", "HIGH")
                .param("windowDays", "30")
                .param("q", "urea")
                .param("sort", "EXPIRY_ASC")
                .param("lowStockThreshold", "5")
                .param("page", "0")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].status").value("ABNORMAL_MOVEMENT"))
                .andExpect(jsonPath("$.result.items[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.result.totalElements").value(1));

        verify(adminInventoryService).listRiskLots(
                eq(1),
                eq(101),
                eq("ABNORMAL_MOVEMENT"),
                eq("HIGH"),
                eq(30),
                eq("urea"),
                eq("EXPIRY_ASC"),
                eq(new java.math.BigDecimal("5")),
                eq(0),
                eq(20));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void listRiskLots_withNonAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory/lots"))
                .andExpect(status().isForbidden());
    }
}

