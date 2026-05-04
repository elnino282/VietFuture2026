package org.example.QuanLyMuaVu.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdatePayrollRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonEmployeeResponse;
import org.example.QuanLyMuaVu.module.season.entity.PayrollRecord;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.SeasonEmployee;
import org.example.QuanLyMuaVu.module.season.repository.PayrollRecordRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonEmployeeRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskProgressLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaborManagementPayrollServiceTest {

    @Mock
    SeasonRepository seasonRepository;
    @Mock
    TaskRepository taskRepository;
    @Mock
    IdentityQueryPort identityQueryPort;
    @Mock
    SeasonEmployeeRepository seasonEmployeeRepository;
    @Mock
    TaskProgressLogRepository taskProgressLogRepository;
    @Mock
    PayrollRecordRepository payrollRecordRepository;
    @Mock
    IncidentCommandPort incidentCommandPort;
    @Mock
    FarmAccessPort farmAccessService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    LaborManagementService laborManagementService;

    @Test
    @DisplayName("Employee can view own payroll detail")
    void getMyPayrollDetail_returnsOwnRecord() {
        User currentEmployee = User.builder().id(101L).username("employee-a").build();
        Season season = buildSeason(11, "Spring 2026");
        PayrollRecord payrollRecord = PayrollRecord.builder()
                .id(55)
                .employee(currentEmployee)
                .season(season)
                .periodStart(LocalDate.of(2026, 3, 1))
                .periodEnd(LocalDate.of(2026, 3, 31))
                .totalAssignedTasks(12)
                .totalCompletedTasks(10)
                .wagePerTask(new BigDecimal("175000"))
                .totalAmount(new BigDecimal("1750000"))
                .generatedAt(LocalDateTime.of(2026, 3, 31, 17, 0))
                .note("Auto-calculated from task completion data.")
                .build();

        when(farmAccessService.getCurrentUser()).thenReturn(currentEmployee);
        when(payrollRecordRepository.findByIdAndEmployee_Id(55, 101L)).thenReturn(Optional.of(payrollRecord));

        PayrollRecordResponse response = laborManagementService.getMyPayrollDetail(55);

        assertEquals(55, response.getId());
        assertEquals(101L, response.getEmployeeUserId());
        assertEquals(10, response.getTotalCompletedTasks());
    }

    @Test
    @DisplayName("Employee cannot view payroll record of other users")
    void getMyPayrollDetail_withOtherEmployeeRecord_throwsNotFound() {
        User currentEmployee = User.builder().id(101L).username("employee-a").build();
        when(farmAccessService.getCurrentUser()).thenReturn(currentEmployee);
        when(payrollRecordRepository.findByIdAndEmployee_Id(55, 101L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> laborManagementService.getMyPayrollDetail(55));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Farmer updates payroll note and action is audited")
    void updateSeasonPayroll_withChangedNote_logsAudit() {
        Season season = buildSeason(20, "Summer 2026");
        User farmer = User.builder().id(900L).username("farmer-a").build();
        User employee = User.builder().id(101L).username("employee-a").build();
        PayrollRecord payrollRecord = PayrollRecord.builder()
                .id(300)
                .employee(employee)
                .season(season)
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .totalAssignedTasks(9)
                .totalCompletedTasks(8)
                .wagePerTask(new BigDecimal("180000"))
                .totalAmount(new BigDecimal("1440000"))
                .generatedAt(LocalDateTime.of(2026, 4, 30, 18, 0))
                .note("Initial note")
                .build();

        when(seasonRepository.findById(20)).thenReturn(Optional.of(season));
        when(payrollRecordRepository.findByIdAndSeason_Id(300, 20)).thenReturn(Optional.of(payrollRecord));
        when(payrollRecordRepository.save(payrollRecord)).thenReturn(payrollRecord);
        when(farmAccessService.getCurrentUser()).thenReturn(farmer);

        PayrollRecordResponse response = laborManagementService.updateSeasonPayroll(
                20,
                300,
                UpdatePayrollRecordRequest.builder().note("  Approved after reconciliation  ").build());

        assertEquals("Approved after reconciliation", response.getNote());

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).logModuleOperation(
                eq("WORKFORCE"),
                eq("PAYROLL_RECORD"),
                eq(300),
                eq("PAYROLL_NOTE_UPDATED"),
                eq("farmer-a"),
                snapshotCaptor.capture(),
                eq("Farmer updated payroll note"),
                isNull());

        Map<String, Object> snapshot = (Map<String, Object>) snapshotCaptor.getValue();
        assertEquals("Initial note", snapshot.get("beforeNote"));
        assertEquals("Approved after reconciliation", snapshot.get("afterNote"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Farmer wage adjustment on season employee is audited")
    void updateSeasonEmployee_withWageAdjustment_logsAudit() {
        Season season = buildSeason(30, "Autumn 2026");
        User farmer = User.builder().id(901L).username("farmer-b").build();
        User employee = User.builder().id(102L).username("employee-b").build();
        SeasonEmployee assignment = SeasonEmployee.builder()
                .id(701)
                .season(season)
                .employee(employee)
                .wagePerTask(new BigDecimal("150000"))
                .active(true)
                .build();

        when(seasonRepository.findById(30)).thenReturn(Optional.of(season));
        when(seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(30, 102L)).thenReturn(Optional.of(assignment));
        when(seasonEmployeeRepository.save(any(SeasonEmployee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(farmAccessService.getCurrentUser()).thenReturn(farmer);

        SeasonEmployeeResponse response = laborManagementService.updateSeasonEmployee(
                30,
                102L,
                UpdateSeasonEmployeeRequest.builder().wagePerTask(new BigDecimal("165000")).build());

        assertEquals(new BigDecimal("165000"), response.getWagePerTask());

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).logModuleOperation(
                eq("WORKFORCE"),
                eq("SEASON_EMPLOYEE"),
                eq(701),
                eq("PAYROLL_WAGE_PER_TASK_UPDATED"),
                eq("farmer-b"),
                snapshotCaptor.capture(),
                eq("Farmer updated wage per task"),
                isNull());

        Map<String, Object> snapshot = (Map<String, Object>) snapshotCaptor.getValue();
        assertEquals(new BigDecimal("150000"), snapshot.get("beforeWagePerTask"));
        assertEquals(new BigDecimal("165000"), snapshot.get("afterWagePerTask"));
    }

    private Season buildSeason(Integer seasonId, String seasonName) {
        Farm farm = Farm.builder().id(1).build();
        Plot plot = Plot.builder().id(2).farm(farm).build();
        return Season.builder()
                .id(seasonId)
                .seasonName(seasonName)
                .plot(plot)
                .status(SeasonStatus.ACTIVE)
                .build();
    }
}
