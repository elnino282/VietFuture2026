package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.inventory.dto.response.DashboardInventoryAlertsResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardDataCompletenessWarningResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardIncidentAlertResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardRecentActivityResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardAlertsService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardPlotStatusReadService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardKpiService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardRecentActivityReadService;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardTaskReadService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardService;
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

        @Mock
        private SustainabilityDashboardService sustainabilityDashboardService;

        @Mock
        private DashboardRecentActivityReadService dashboardRecentActivityReadService;

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
        @DisplayName("GetTodayTasks - Returns empty page when there is no task")
        void getTodayTasks_WhenNoTask_ReturnsEmptyPage() {
                // Arrange
                Pageable pageable = PageRequest.of(0, 10);
                Page<TodayTaskResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                when(dashboardTaskReadService.getTodayTasks(1, pageable)).thenReturn(emptyPage);

                // Act
                Page<TodayTaskResponse> result = dashboardService.getTodayTasks(1, pageable);

                // Assert
                assertNotNull(result);
                assertTrue(result.isEmpty());
                assertEquals(0, result.getTotalElements());
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

        @Test
        @DisplayName("GetInventoryAlerts - Delegates to AlertsService")
        void getInventoryAlerts_DelegatesToAlertsService() {
                // Arrange
                when(alertsService.getInventoryAlerts(20))
                                .thenReturn(DashboardInventoryAlertsResponse.builder().build());

                // Act
                dashboardService.getInventoryAlerts(20);

                // Assert
                verify(alertsService).getInventoryAlerts(20);
        }

        @Test
        @DisplayName("GetDataCompletenessWarnings - Returns warning list for missing sustainability inputs")
        void getDataCompletenessWarnings_WithMissingInputs_ReturnsWarnings() {
                // Arrange
                when(sustainabilityDashboardService.getOverview("field", 1, null, null))
                                .thenReturn(SustainabilityOverviewResponse.builder()
                                                .seasonId(1)
                                                .missingInputs(List.of("MINERAL_FERTILIZER", "IRRIGATION_WATER"))
                                                .build());

                // Act
                List<DashboardDataCompletenessWarningResponse> warnings = dashboardService.getDataCompletenessWarnings(1);

                // Assert
                assertNotNull(warnings);
                assertEquals(2, warnings.size());
                assertEquals("DATA_COMPLETENESS", warnings.get(0).getType());
                assertEquals("ACTION_REQUIRED", warnings.get(0).getStatus());
                assertEquals("MINERAL_FERTILIZER", warnings.get(0).getInputCode());
                assertEquals("/farmer/seasons/1/workspace/nutrient-inputs", warnings.get(0).getActionTarget());
                assertEquals("/farmer/seasons/1/workspace/irrigation-water-analyses", warnings.get(1).getActionTarget());
        }

        @Test
        @DisplayName("GetIncidentAlerts - Delegates to AlertsService and validates ownership")
        void getIncidentAlerts_DelegatesAndValidates() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
                when(ownershipService.requireOwnedSeason(1)).thenReturn(testSeason);
                when(alertsService.getIncidentAlerts(1)).thenReturn(List.of(
                                DashboardIncidentAlertResponse.builder()
                                                .id("incident-11")
                                                .type("OPEN_INCIDENT")
                                                .severity("HIGH")
                                                .title("Open incident requires attention")
                                                .seasonId(1)
                                                .plotId(1)
                                                .build()));

                // Act
                List<DashboardIncidentAlertResponse> alerts = dashboardService.getIncidentAlerts(1);

                // Assert
                assertEquals(1, alerts.size());
                assertEquals("incident-11", alerts.get(0).getId());
                verify(ownershipService).requireOwnedSeason(1);
                verify(alertsService).getIncidentAlerts(1);
        }

        @Test
        @DisplayName("GetRecentActivities - Delegates to read service with owner scope")
        void getRecentActivities_DelegatesToReadService() {
                // Arrange
                when(currentUserService.getCurrentUserId()).thenReturn(1L);
                when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
                when(dashboardRecentActivityReadService.getRecentActivities(1L, 10)).thenReturn(List.of(
                                DashboardRecentActivityResponse.builder()
                                                .id("task-progress-7")
                                                .type("TASK_UPDATE")
                                                .title("Irrigation task updated")
                                                .description("Progress updated to 75%")
                                                .entityType("TASK")
                                                .entityId("7")
                                                .build()));

                // Act
                List<DashboardRecentActivityResponse> activities = dashboardService.getRecentActivities(10);

                // Assert
                assertEquals(1, activities.size());
                assertEquals("TASK_UPDATE", activities.get(0).getType());
                verify(dashboardRecentActivityReadService).getRecentActivities(1L, 10);
        }

        @Test
        @DisplayName("GetDashboardTasksAndWarnings - Supports both real task and data completeness warning")
        void getDashboardTasksAndWarnings_WithTaskAndWarning_ReturnsBoth() {
                // Arrange
                Pageable pageable = PageRequest.of(0, 10);
                TodayTaskResponse realTask = TodayTaskResponse.builder()
                                .taskId(91)
                                .title("Apply fertilizer")
                                .plotName("Field Z")
                                .status(TaskStatus.PENDING.name())
                                .dueDate(LocalDate.now().plusDays(1))
                                .build();
                when(dashboardTaskReadService.getTodayTasks(1, pageable))
                                .thenReturn(new PageImpl<>(List.of(realTask), pageable, 1));
                when(sustainabilityDashboardService.getOverview("field", 1, null, null))
                                .thenReturn(SustainabilityOverviewResponse.builder()
                                                .seasonId(1)
                                                .missingInputs(List.of("SOIL_LEGACY"))
                                                .build());

                // Act
                Page<TodayTaskResponse> taskPage = dashboardService.getTodayTasks(1, pageable);
                List<DashboardDataCompletenessWarningResponse> warnings = dashboardService.getDataCompletenessWarnings(1);

                // Assert
                assertEquals(1, taskPage.getTotalElements());
                assertEquals("Apply fertilizer", taskPage.getContent().get(0).getTitle());
                assertEquals(1, warnings.size());
                assertEquals("SOIL_LEGACY", warnings.get(0).getInputCode());
                assertEquals("/farmer/seasons/1/workspace/soil-tests", warnings.get(0).getActionTarget());
        }
}
