package org.example.QuanLyMuaVu.module.season.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdatePayrollRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FarmerLaborManagementController.class)
@Import(FarmerLaborManagementControllerTest.MethodSecurityTestConfig.class)
class FarmerLaborManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LaborManagementService laborManagementService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getSeasonPayrollDetail_withFarmerRole_returnsRecord() throws Exception {
        PayrollRecordResponse payload = PayrollRecordResponse.builder()
                .id(41)
                .seasonId(7)
                .seasonName("Summer 2026")
                .employeeUserId(201L)
                .employeeName("Worker X")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .totalAssignedTasks(8)
                .totalCompletedTasks(7)
                .wagePerTask(new BigDecimal("190000"))
                .totalAmount(new BigDecimal("1330000"))
                .generatedAt(LocalDateTime.of(2026, 4, 30, 18, 0))
                .note("Reviewed")
                .build();

        when(laborManagementService.getSeasonPayrollDetail(7, 41)).thenReturn(payload);

        mockMvc.perform(get("/api/v1/farmer/labor/seasons/7/payroll/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(41))
                .andExpect(jsonPath("$.result.employeeUserId").value(201))
                .andExpect(jsonPath("$.result.note").value("Reviewed"));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void updateSeasonPayroll_withFarmerRole_updatesNote() throws Exception {
        UpdatePayrollRecordRequest request = UpdatePayrollRecordRequest.builder().note("Approved for payout").build();
        PayrollRecordResponse payload = PayrollRecordResponse.builder()
                .id(41)
                .seasonId(7)
                .employeeUserId(201L)
                .note("Approved for payout")
                .build();

        when(laborManagementService.updateSeasonPayroll(eq(7), eq(41), any(UpdatePayrollRecordRequest.class)))
                .thenReturn(payload);

        mockMvc.perform(patch("/api/v1/farmer/labor/seasons/7/payroll/41")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(41))
                .andExpect(jsonPath("$.result.note").value("Approved for payout"));

        verify(laborManagementService).updateSeasonPayroll(eq(7), eq(41), any(UpdatePayrollRecordRequest.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateSeasonPayroll_withNonFarmerRole_returnsForbidden() throws Exception {
        UpdatePayrollRecordRequest request = UpdatePayrollRecordRequest.builder().note("Not allowed").build();

        mockMvc.perform(patch("/api/v1/farmer/labor/seasons/7/payroll/41")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
