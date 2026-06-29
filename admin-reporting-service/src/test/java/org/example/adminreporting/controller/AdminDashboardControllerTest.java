package org.example.adminreporting.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.example.adminreporting.config.CustomJwtDecoder;
import org.example.adminreporting.dto.response.AdminPendingApprovalItemDTO;
import org.example.adminreporting.dto.response.DashboardStatsDTO;
import org.example.adminreporting.dto.response.AdminInventoryHealthResponse;
import org.example.adminreporting.service.AdminDashboardService;
import org.example.adminreporting.service.AdminInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminDashboardController.class)
@org.springframework.context.annotation.Import(AdminDashboardControllerTest.MethodSecurityTestConfig.class)
class AdminDashboardControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminInventoryService adminInventoryService;

    @MockBean
    private CustomJwtDecoder customJwtDecoder;

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
        when(adminDashboardService.getPendingApprovals(eq(10))).thenReturn(items);

        mockMvc.perform(get("/api/v1/admin/dashboard/pending-approvals").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].type").value("PAYMENT_PROOF_VERIFICATION"))
                .andExpect(jsonPath("$.result[0].actionUrl").value("/admin/marketplace-orders?orderId=501"));

        verify(adminDashboardService).getPendingApprovals(eq(10));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getPendingApprovals_withNonAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/pending-approvals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboardStats_returnsStats() throws Exception {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .summary(DashboardStatsDTO.Summary.builder()
                        .totalFarms(5L)
                        .totalSeasons(10L)
                        .build())
                .build();
        when(adminDashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.summary.totalFarms").value(5))
                .andExpect(jsonPath("$.result.summary.totalSeasons").value(10));
    }
}
