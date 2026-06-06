package org.example.QuanLyMuaVu.module.season.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.ai.service.DiseaseSuggestionService;
import org.example.QuanLyMuaVu.module.inventory.service.SuppliesService;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.service.DiseaseRecordService;
import org.example.QuanLyMuaVu.module.season.service.FieldLogService;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
import org.example.QuanLyMuaVu.module.season.service.SeasonWorkspaceAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmployeePortalController.class)
@Import(EmployeePortalControllerTest.MethodSecurityTestConfig.class)
class EmployeePortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LaborManagementService laborManagementService;

    @MockBean
    private FieldLogService fieldLogService;

    @MockBean
    private DiseaseRecordService diseaseRecordService;

    @MockBean
    private DiseaseSuggestionService diseaseSuggestionService;

    @MockBean
    private SuppliesService suppliesService;

    @MockBean
    private SeasonWorkspaceAccessService seasonWorkspaceAccessService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getMyPayrollDetail_withEmployeeRole_returnsRecord() throws Exception {
        PayrollRecordResponse payload = PayrollRecordResponse.builder()
                .id(15)
                .seasonId(3)
                .seasonName("Spring 2026")
                .employeeUserId(101L)
                .employeeName("Worker A")
                .periodStart(LocalDate.of(2026, 3, 1))
                .periodEnd(LocalDate.of(2026, 3, 31))
                .totalAssignedTasks(12)
                .totalCompletedTasks(10)
                .wagePerTask(new BigDecimal("175000"))
                .totalAmount(new BigDecimal("1750000"))
                .generatedAt(LocalDateTime.of(2026, 3, 31, 18, 30))
                .note("Reviewed")
                .build();

        when(laborManagementService.getMyPayrollDetail(15)).thenReturn(payload);

        mockMvc.perform(get("/api/v1/employee/payroll/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(15))
                .andExpect(jsonPath("$.result.employeeUserId").value(101))
                .andExpect(jsonPath("$.result.totalCompletedTasks").value(10));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getMyPayrollDetail_withNonEmployeeRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/employee/payroll/15"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyPayrollDetail_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/employee/payroll/15"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getMyPayrollDetail_whenNotOwner_returnsNotFound() throws Exception {
        when(laborManagementService.getMyPayrollDetail(15))
                .thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/employee/payroll/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR_RESOURCE_NOT_FOUND"));
    }
}
