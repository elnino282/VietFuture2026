package org.example.QuanLyMuaVu.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.season.entity.Season;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaborManagementServiceLockTest {

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
    @DisplayName("recalculatePayroll rejects locked seasons")
    void recalculatePayroll_rejectsLockedSeason() {
        Farm farm = Farm.builder().id(1).build();
        Plot plot = Plot.builder().id(2).farm(farm).build();
        Season season = Season.builder()
                .id(10)
                .plot(plot)
                .status(SeasonStatus.COMPLETED)
                .build();

        when(seasonRepository.findById(10)).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);

        AppException exception = assertThrows(
                AppException.class,
                () -> laborManagementService.recalculatePayroll(
                        10,
                        null,
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31)));

        assertEquals(ErrorCode.INVALID_SEASON_STATUS_TRANSITION, exception.getErrorCode());
        verify(seasonRepository).findById(10);
        verifyNoInteractions(seasonEmployeeRepository);
    }
}
