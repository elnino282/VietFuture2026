package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardAlertsService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardPlotStatusReadService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardKpiService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardTaskReadService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for DashboardService.
 * 
 * Covers key operations: get overview, get today tasks, get plot status.
 */
@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private FarmerOwnershipService ownershipService;

        @Mock
        private FarmQueryPort farmQueryPort;

        @Mock
        private SeasonQueryPort seasonQueryPort;

        @Mock
        private DashboardTaskReadService dashboardTaskReadService;

        @Mock
        private DashboardPlotStatusReadService dashboardPlotStatusReadService;

        @Mock
        private IdentityQueryPort identityQueryPort;

        @Mock
        private DashboardKpiService kpiService;

        @Mock
        private DashboardAlertsService alertsService;

        @InjectMocks
        private DashboardService dashboardService;

        private User testUser;
        private Season testSeason;
        private Farm testFarm;
        private Plot testPlot;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .username("farmer")
                                .email("farmer@test.com")
                                .build();

                testFarm = Farm.builder()
                                .id(1)
                                .name("Test Farm")
                                .user(testUser)
                                .active(true)
                                .build();

                testPlot = Plot.builder()
                                .id(1)
                                .plotName("Test Plot")
                                .farm(testFarm)
                                .area(BigDecimal.valueOf(10))
                                .build();

                testSeason = Season.builder()
                                .id(1)
                                .seasonName("Spring 2024")
                                .plot(testPlot)
                                .status(SeasonStatus.ACTIVE)
                                .startDate(LocalDate.now().minusMonths(1))
                                .endDate(LocalDate.now().plusMonths(2))
                                .build();
        }

        @Test
        @DisplayName("GetOverview - Returns dashboard with season context")
        void getOverview_WithSeasonId_ReturnsDashboardOverview() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
                when(ownershipService.requireOwnedSeason(1)).thenReturn(testSeason);
                when(farmQueryPort.countActiveFarmsByOwnerId(1L)).thenReturn(2L);
                when(farmQueryPort.countPlotsByOwnerId(1L)).thenReturn(5L);
                when(seasonQueryPort.countSeasonsByStatusAndOwnerId(any(SeasonStatus.class), eq(1L))).thenReturn(1L);
                when(kpiService.buildKpis(testSeason)).thenReturn(null);
                when(kpiService.buildExpenses(testSeason)).thenReturn(null);
                when(kpiService.buildHarvest(testSeason)).thenReturn(null);
                when(alertsService.buildAlerts(1L)).thenReturn(null);

                // Act
                DashboardOverviewResponse response = dashboardService.getOverview(1);

                // Assert
                assertNotNull(response);
                assertNotNull(response.getSeasonContext());
                assertEquals(1, response.getSeasonContext().getSeasonId());
                assertEquals("Spring 2024", response.getSeasonContext().getSeasonName());
                assertNotNull(response.getCounts());
                assertEquals(2, response.getCounts().getActiveFarms());
                assertEquals(5, response.getCounts().getActivePlots());
        }

        @Test
        @DisplayName("GetOverview - Throws USER_NOT_FOUND when user doesn't exist")
        void getOverview_WhenUserNotFound_ThrowsAppException() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(999L);
                when(identityQueryPort.findUserById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> dashboardService.getOverview(null));

                assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("GetTodayTasks - Returns paginated today tasks")
        void getTodayTasks_ReturnsPagedTasks() {
                // Arrange
                Pageable pageable = PageRequest.of(0, 10);
                TodayTaskResponse task = TodayTaskResponse.builder()
                                .taskId(1)
                                .title("Water Plants")
                                .plotName("Test Plot")
                                .status(TaskStatus.PENDING.name())
                                .dueDate(LocalDate.now())
                                .build();
                Page<TodayTaskResponse> taskPage = new PageImpl<>(List.of(task), pageable, 1);
                when(dashboardTaskReadService.getTodayTasks(null, pageable)).thenReturn(taskPage);

                // Act
                Page<TodayTaskResponse> result = dashboardService.getTodayTasks(null, pageable);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Water Plants", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("GetUpcomingTasks - Returns tasks within N days")
        void getUpcomingTasks_ReturnsTasksWithinDays() {
                // Arrange
                TodayTaskResponse task = TodayTaskResponse.builder()
                                .taskId(1)
                                .title("Apply Fertilizer")
                                .plotName("Field A")
                                .status(TaskStatus.PENDING.name())
                                .dueDate(LocalDate.now().plusDays(3))
                                .build();
                when(dashboardTaskReadService.getUpcomingTasks(7, null)).thenReturn(List.of(task));

                // Act
                List<TodayTaskResponse> result = dashboardService.getUpcomingTasks(7, null);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("Apply Fertilizer", result.get(0).getTitle());
        }

        @Test
        @DisplayName("GetLowStock - Delegates to AlertsService")
        void getLowStock_DelegatesToAlertsService() {
                // Arrange
                when(alertsService.getLowStock(5)).thenReturn(List.of());

                // Act
                dashboardService.getLowStock(5);

                // Assert
                verify(alertsService).getLowStock(5);
        }
}
