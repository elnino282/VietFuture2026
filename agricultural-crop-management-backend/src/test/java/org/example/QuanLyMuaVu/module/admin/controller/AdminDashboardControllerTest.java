package org.example.QuanLyMuaVu.module.admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminPendingApprovalItemDTO;
import org.example.QuanLyMuaVu.module.admin.service.AdminDashboardFacade;
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

@WebMvcTest(controllers = AdminDashboardController.class)
@Import(AdminDashboardControllerTest.MethodSecurityTestConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminDashboardFacade adminDashboardFacade;
    @MockBean
    AdminInventoryService adminInventoryService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPendingApprovals_withAdminRole_returnsRealItems() throws Exception {
        List<AdminPendingApprovalItemDTO> items = List.of(
                AdminPendingApprovalItemDTO.builder()
                        .id(501L)
                        .type("PAYMENT_PROOF_VERIFICATION")
                        .title("Verify payment proof")
                        .subtitle("Order ORD-501 | Buyer #22")
                        .submittedAt(LocalDateTime.of(2026, 5, 10, 9, 30))
                        .priority("HIGH")
                        .severity("HIGH")
                        .actionUrl("/admin/marketplace-orders?orderId=501")
                        .actionTarget("PAYMENT_PROOF_VERIFICATION")
                        .build());
        when(adminDashboardFacade.getPendingApprovals(eq(10))).thenReturn(items);

        mockMvc.perform(get("/api/v1/admin/dashboard/pending-approvals").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].type").value("PAYMENT_PROOF_VERIFICATION"))
                .andExpect(jsonPath("$.result[0].actionUrl").value("/admin/marketplace-orders?orderId=501"));

        verify(adminDashboardFacade).getPendingApprovals(eq(10));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getPendingApprovals_withNonAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/pending-approvals"))
                .andExpect(status().isForbidden());
    }
}
