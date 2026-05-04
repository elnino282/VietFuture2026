package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.season.mapper.SeasonMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.example.QuanLyMuaVu.module.season.service.SeasonQueryService;
import org.example.QuanLyMuaVu.module.season.service.SeasonService;
import org.example.QuanLyMuaVu.module.season.service.SeasonStatusService;
import org.example.QuanLyMuaVu.module.season.service.SeasonValidationService;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SeasonService.
 * 
 * Tests cover:
 * - CREATE SEASON: Happy path, plot not found, crop not found, variety
 * validation,
 * date validation, overlapping seasons
 * - UPDATE SEASON: Happy path, season not found, invalid status for update
 * - DELETE SEASON: Happy path, season not found, invalid status, has child
 * records
 * - LEGACY METHODS: create(), get(), update(), delete()
 * - DELEGATION: Query, status, and validation service delegation verification
 * 
 * Follows AAA (Arrange-Act-Assert) pattern.
 */
@ExtendWith(MockitoExtension.class)
public class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private FarmQueryPort farmQueryPort;

    @Mock
    private CropCatalogQueryPort cropCatalogQueryPort;

    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private ExpenseQueryPort expenseQueryPort;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private FieldLogRepository fieldLogRepository;

    @Mock
    private SeasonMapper seasonMapper;

    @Mock
    private SeasonQueryService queryService;

    @Mock
    private SeasonStatusService statusService;

    @Mock
    private SeasonValidationService validationService;

    @Mock
    private FarmAccessPort farmAccessService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private SeasonService seasonService;

    // Test fixtures
    private User testUser;
    private Farm testFarm;
    private Plot testPlot;
    private Crop testCrop;
    private Variety testVariety;
    private Season testSeason;
    private CreateSeasonRequest createRequest;
    private UpdateSeasonRequest updateRequest;
    private SeasonDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        // Arrange: Create test data hierarchy
        testUser = User.builder()
                .id(1L)
                .username("farmer")
                .email("farmer@test.com")
                .build();

        testFarm = Farm.builder()
                .id(1)
                .name("Test Farm")
                .user(testUser)
                .area(BigDecimal.valueOf(100))
                .active(true)
                .build();

        testPlot = Plot.builder()
                .id(1)
                .plotName("Test Plot")
                .farm(testFarm)
                .user(testUser)
                .area(BigDecimal.valueOf(10))
                .build();

        testCrop = Crop.builder()
                .id(1)
                .cropName("Rice")
                .build();

        testVariety = Variety.builder()
                .id(1)
                .name("Jasmine Rice")
                .crop(testCrop)
                .build();

        testSeason = Season.builder()
                .id(1)
                .seasonName("Spring 2024")
                .plot(testPlot)
                .crop(testCrop)
                .variety(testVariety)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status(SeasonStatus.PLANNED)
                .initialPlantCount(1000)
                .currentPlantCount(1000)
                .build();

        createRequest = CreateSeasonRequest.builder()
                .plotId(1)
                .cropId(1)
                .varietyId(1)
                .seasonName("Spring 2024")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .plannedHarvestDate(LocalDate.now().plusMonths(2))
                .initialPlantCount(1000)
                .expectedYieldKg(BigDecimal.valueOf(5000))
                .notes("Test notes")
                .build();

        updateRequest = UpdateSeasonRequest.builder()
                .seasonName("Updated Season")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(4))
                .plannedHarvestDate(LocalDate.now().plusMonths(3))
                .currentPlantCount(950)
                .expectedYieldKg(BigDecimal.valueOf(4500))
                .notes("Updated notes")
                .build();

        detailResponse = SeasonDetailResponse.builder()
                .id(1)
                .seasonName("Spring 2024")
                .plotId(1)
                .plotName("Test Plot")
                .cropId(1)
                .cropName("Rice")
                .status("PLANNED")
                .build();
    }

    // =========================================================================
    // CREATE SEASON TESTS
    // =========================================================================

    @Nested
    @DisplayName("createSeason() Tests")
    class CreateSeasonTests {

        @Test
        @DisplayName("Happy Path: Creates season with valid request")
        void createSeason_WithValidRequest_ReturnsSeasonDetailResponse() {
            // Arrange
            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.of(testPlot));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessPlot(testPlot);
            when(cropCatalogQueryPort.findCropById(1)).thenReturn(Optional.of(testCrop));
            when(cropCatalogQueryPort.findVarietyById(1)).thenReturn(Optional.of(testVariety));
            doNothing().when(validationService).validateSeasonDates(any(), any(), any());
            doNothing().when(validationService).validateNoOverlappingActiveOrPlannedSeasons(any(), any(), any(), any(),
                    any());

            ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);
            when(seasonRepository.save(seasonCaptor.capture())).thenAnswer(invocation -> {
                Season s = invocation.getArgument(0);
                s.setId(1);
                return s;
            });
            when(seasonMapper.toDetailResponse(any())).thenReturn(detailResponse);

            // Act
            SeasonDetailResponse response = seasonService.createSeason(createRequest);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getId());
            assertEquals("Spring 2024", response.getSeasonName());

            // Verify captured season has correct values
            Season captured = seasonCaptor.getValue();
            assertEquals("Spring 2024", captured.getSeasonName());
            assertEquals(testPlot, captured.getPlot());
            assertEquals(testCrop, captured.getCrop());
            assertEquals(testVariety, captured.getVariety());
            assertEquals(SeasonStatus.PLANNED, captured.getStatus());
            assertEquals(1000, captured.getInitialPlantCount());
            assertEquals(1000, captured.getCurrentPlantCount());

            verify(farmAccessService).assertCurrentUserCanAccessPlot(testPlot);
            verify(validationService).validateSeasonDates(any(), any(), any());
        }

        @Test
        @DisplayName("Happy Path: Creates season without variety (variety is optional)")
        void createSeason_WithoutVariety_CreatesSuccessfully() {
            // Arrange
            CreateSeasonRequest requestWithoutVariety = CreateSeasonRequest.builder()
                    .plotId(1)
                    .cropId(1)
                    .varietyId(null) // No variety
                    .seasonName("Spring 2024")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(3))
                    .initialPlantCount(1000)
                    .build();

            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.of(testPlot));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessPlot(any());
            when(cropCatalogQueryPort.findCropById(1)).thenReturn(Optional.of(testCrop));
            doNothing().when(validationService).validateSeasonDates(any(), any(), any());
            doNothing().when(validationService).validateNoOverlappingActiveOrPlannedSeasons(any(), any(), any(), any(),
                    any());
            when(seasonRepository.save(any())).thenAnswer(i -> {
                Season s = i.getArgument(0);
                s.setId(1);
                return s;
            });
            when(seasonMapper.toDetailResponse(any())).thenReturn(detailResponse);

            // Act
            SeasonDetailResponse response = seasonService.createSeason(requestWithoutVariety);

            // Assert
            assertNotNull(response);
            verify(cropCatalogQueryPort, never()).findVarietyById(any());
        }

        @Test
        @DisplayName("Negative Case: Throws PLOT_NOT_FOUND when plot doesn't exist")
        void createSeason_WhenPlotNotFound_ThrowsAppException() {
            // Arrange
            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.createSeason(createRequest));

            assertEquals(ErrorCode.PLOT_NOT_FOUND, exception.getErrorCode());
            verify(cropCatalogQueryPort, never()).findCropById(any());
        }

        @Test
        @DisplayName("Negative Case: Throws CROP_NOT_FOUND when crop doesn't exist")
        void createSeason_WhenCropNotFound_ThrowsAppException() {
            // Arrange
            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.of(testPlot));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessPlot(any());
            when(cropCatalogQueryPort.findCropById(1)).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.createSeason(createRequest));

            assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws RESOURCE_NOT_FOUND when variety doesn't exist")
        void createSeason_WhenVarietyNotFound_ThrowsAppException() {
            // Arrange
            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.of(testPlot));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessPlot(any());
            when(cropCatalogQueryPort.findCropById(1)).thenReturn(Optional.of(testCrop));
            when(cropCatalogQueryPort.findVarietyById(1)).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.createSeason(createRequest));

            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws BAD_REQUEST when variety doesn't belong to crop")
        void createSeason_WhenVarietyDoesNotBelongToCrop_ThrowsAppException() {
            // Arrange
            Crop differentCrop = Crop.builder().id(2).cropName("Wheat").build();
            Variety varietyOfDifferentCrop = Variety.builder()
                    .id(1)
                    .name("Winter Wheat")
                    .crop(differentCrop) // Different crop
                    .build();

            when(farmQueryPort.findPlotById(1)).thenReturn(Optional.of(testPlot));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessPlot(any());
            when(cropCatalogQueryPort.findCropById(1)).thenReturn(Optional.of(testCrop)); // Rice (id=1)
            when(cropCatalogQueryPort.findVarietyById(1)).thenReturn(Optional.of(varietyOfDifferentCrop));

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.createSeason(createRequest));

            assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        }
    }

    // =========================================================================
    // UPDATE SEASON TESTS
    // =========================================================================

    @Nested
    @DisplayName("updateSeason() Tests")
    class UpdateSeasonTests {

        @Test
        @DisplayName("Happy Path: Updates season with valid request")
        void updateSeason_WithValidRequest_ReturnsUpdatedResponse() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            doNothing().when(validationService).validateSeasonDates(any(), any(), any());
            doNothing().when(validationService).validateNoOverlappingActiveOrPlannedSeasons(any(), any(), any(), any(),
                    eq(1));
            when(seasonRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(seasonMapper.toDetailResponse(any())).thenReturn(detailResponse);

            // Act
            SeasonDetailResponse response = seasonService.updateSeason(1, updateRequest);

            // Assert
            assertNotNull(response);
            verify(seasonRepository).save(any());
        }

        @Test
        @DisplayName("Negative Case: Throws SEASON_NOT_FOUND when season doesn't exist")
        void updateSeason_WhenSeasonNotFound_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.updateSeason(999, updateRequest));

            assertEquals(ErrorCode.SEASON_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when trying to update COMPLETED season")
        void updateSeason_WhenStatusCompleted_ThrowsAppException() {
            // Arrange
            Season completedSeason = Season.builder()
                    .id(1)
                    .seasonName("Completed Season")
                    .plot(testPlot)
                    .crop(testCrop)
                    .status(SeasonStatus.COMPLETED)
                    .build();

            when(seasonRepository.findById(1)).thenReturn(Optional.of(completedSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(completedSeason);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.updateSeason(1, updateRequest));

            assertEquals(ErrorCode.INVALID_SEASON_STATUS_TRANSITION, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when trying to update CANCELLED season")
        void updateSeason_WhenStatusCancelled_ThrowsAppException() {
            // Arrange
            Season cancelledSeason = Season.builder()
                    .id(1)
                    .seasonName("Cancelled Season")
                    .plot(testPlot)
                    .crop(testCrop)
                    .status(SeasonStatus.CANCELLED)
                    .build();

            when(seasonRepository.findById(1)).thenReturn(Optional.of(cancelledSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(cancelledSeason);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.updateSeason(1, updateRequest));

            assertEquals(ErrorCode.INVALID_SEASON_STATUS_TRANSITION, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when trying to update ARCHIVED season")
        void updateSeason_WhenStatusArchived_ThrowsAppException() {
            // Arrange
            Season archivedSeason = Season.builder()
                    .id(1)
                    .seasonName("Archived Season")
                    .plot(testPlot)
                    .crop(testCrop)
                    .status(SeasonStatus.ARCHIVED)
                    .build();

            when(seasonRepository.findById(1)).thenReturn(Optional.of(archivedSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(archivedSeason);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.updateSeason(1, updateRequest));

            assertEquals(ErrorCode.INVALID_SEASON_STATUS_TRANSITION, exception.getErrorCode());
        }

        @Test
        @DisplayName("Happy Path: Updates season with new variety")
        void updateSeason_WithNewVariety_UpdatesVariety() {
            // Arrange
            Variety newVariety = Variety.builder()
                    .id(2)
                    .name("Wild Rice")
                    .crop(testCrop) // Same crop
                    .build();

            UpdateSeasonRequest requestWithVariety = UpdateSeasonRequest.builder()
                    .seasonName("Updated")
                    .startDate(LocalDate.now())
                    .varietyId(2)
                    .build();

            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());
            doNothing().when(validationService).validateSeasonDates(any(), any(), any());
            doNothing().when(validationService).validateNoOverlappingActiveOrPlannedSeasons(any(), any(), any(), any(),
                    any());
            when(cropCatalogQueryPort.findVarietyById(2)).thenReturn(Optional.of(newVariety));
            when(seasonRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(seasonMapper.toDetailResponse(any())).thenReturn(detailResponse);

            // Act
            seasonService.updateSeason(1, requestWithVariety);

            // Assert
            verify(cropCatalogQueryPort).findVarietyById(2);
            verify(seasonRepository)
                    .save(argThat(season -> season.getVariety() != null && season.getVariety().getId().equals(2)));
        }
    }

    // =========================================================================
    // DELETE SEASON TESTS
    // =========================================================================

    @Nested
    @DisplayName("deleteSeason() Tests")
    class DeleteSeasonTests {

        @Test
        @DisplayName("Happy Path: Deletes PLANNED season with no child records")
        void deleteSeason_WhenPlannedAndNoChildren_DeletesSuccessfully() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            when(taskRepository.existsBySeason_Id(1)).thenReturn(false);
            when(fieldLogRepository.existsBySeason_Id(1)).thenReturn(false);
            when(harvestRepository.existsBySeason_Id(1)).thenReturn(false);
            when(expenseQueryPort.existsExpenseBySeasonId(1)).thenReturn(false);
            doNothing().when(seasonRepository).delete(testSeason);

            // Act
            assertDoesNotThrow(() -> seasonService.deleteSeason(1));

            // Assert
            verify(seasonRepository).delete(testSeason);
        }

        @Test
        @DisplayName("Negative Case: Throws SEASON_NOT_FOUND when season doesn't exist")
        void deleteSeason_WhenSeasonNotFound_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(999));

            assertEquals(ErrorCode.SEASON_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when season is ACTIVE")
        void deleteSeason_WhenStatusActive_ThrowsAppException() {
            // Arrange
            Season activeSeason = Season.builder()
                    .id(1)
                    .seasonName("Active Season")
                    .plot(testPlot)
                    .crop(testCrop)
                    .status(SeasonStatus.ACTIVE)
                    .build();

            when(seasonRepository.findById(1)).thenReturn(Optional.of(activeSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(activeSeason);

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(1));

            assertEquals(ErrorCode.SEASON_HAS_CHILD_RECORDS, exception.getErrorCode());
            verify(seasonRepository, never()).delete(any(Season.class));
        }

        @Test
        @DisplayName("Negative Case: Throws error when season has tasks")
        void deleteSeason_WhenHasTasks_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            when(taskRepository.existsBySeason_Id(1)).thenReturn(true); // Has tasks

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(1));

            assertEquals(ErrorCode.SEASON_HAS_CHILD_RECORDS, exception.getErrorCode());
            verify(seasonRepository, never()).delete(any(Season.class));
        }

        @Test
        @DisplayName("Negative Case: Throws error when season has field logs")
        void deleteSeason_WhenHasFieldLogs_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            when(taskRepository.existsBySeason_Id(1)).thenReturn(false);
            when(fieldLogRepository.existsBySeason_Id(1)).thenReturn(true); // Has field logs

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(1));

            assertEquals(ErrorCode.SEASON_HAS_CHILD_RECORDS, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when season has harvests")
        void deleteSeason_WhenHasHarvests_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            when(taskRepository.existsBySeason_Id(1)).thenReturn(false);
            when(fieldLogRepository.existsBySeason_Id(1)).thenReturn(false);
            when(harvestRepository.existsBySeason_Id(1)).thenReturn(true); // Has harvests

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(1));

            assertEquals(ErrorCode.SEASON_HAS_CHILD_RECORDS, exception.getErrorCode());
        }

        @Test
        @DisplayName("Negative Case: Throws error when season has expenses")
        void deleteSeason_WhenHasExpenses_ThrowsAppException() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
            when(taskRepository.existsBySeason_Id(1)).thenReturn(false);
            when(fieldLogRepository.existsBySeason_Id(1)).thenReturn(false);
            when(harvestRepository.existsBySeason_Id(1)).thenReturn(false);
            when(expenseQueryPort.existsExpenseBySeasonId(1)).thenReturn(true); // Has expenses

            // Act & Assert
            AppException exception = assertThrows(AppException.class,
                    () -> seasonService.deleteSeason(1));

            assertEquals(ErrorCode.SEASON_HAS_CHILD_RECORDS, exception.getErrorCode());
        }
    }

    // =========================================================================
    // LEGACY METHODS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Legacy Methods Tests")
    class LegacyMethodsTests {

        @Test
        @DisplayName("getAll() returns all seasons from repository")
        void getAll_ReturnsAllSeasons() {
            // Arrange
            Season season2 = Season.builder().id(2).seasonName("Season 2").build();
            when(seasonRepository.findAll()).thenReturn(List.of(testSeason, season2));

            // Act
            List<Season> result = seasonService.getAll();

            // Assert
            assertEquals(2, result.size());
            verify(seasonRepository).findAll();
        }

        @Test
        @DisplayName("getAll() returns empty list when no seasons")
        void getAll_WhenNoSeasons_ReturnsEmptyList() {
            // Arrange
            when(seasonRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Season> result = seasonService.getAll();

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getById() returns season when found")
        void getById_WhenFound_ReturnsSeason() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));

            // Act
            Season result = seasonService.getById(1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getId());
        }

        @Test
        @DisplayName("getById() returns null when not found")
        void getById_WhenNotFound_ReturnsNull() {
            // Arrange
            when(seasonRepository.findById(999)).thenReturn(Optional.empty());

            // Act
            Season result = seasonService.getById(999);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("delete() calls deleteSeason()")
        void delete_CallsDeleteSeason() {
            // Arrange
            when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
            doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());
            when(taskRepository.existsBySeason_Id(1)).thenReturn(false);
            when(fieldLogRepository.existsBySeason_Id(1)).thenReturn(false);
            when(harvestRepository.existsBySeason_Id(1)).thenReturn(false);
            when(expenseQueryPort.existsExpenseBySeasonId(1)).thenReturn(false);

            // Act
            seasonService.delete(1);

            // Assert
            verify(seasonRepository).delete(testSeason);
        }
    }

    // =========================================================================
    // DELEGATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Service Delegation Tests")
    class DelegationTests {

        @Test
        @DisplayName("getMySeasons() delegates to QueryService")
        void getMySeasons_DelegatesToQueryService() {
            // Arrange
            when(queryService.getMySeasons()).thenReturn(Collections.emptyList());

            // Act
            seasonService.getMySeasons();

            // Assert
            verify(queryService).getMySeasons();
        }

        @Test
        @DisplayName("getSeasonById() delegates to QueryService")
        void getSeasonById_DelegatesToQueryService() {
            // Arrange
            when(queryService.getSeasonById(1)).thenReturn(testSeason);

            // Act
            Season result = seasonService.getSeasonById(1);

            // Assert
            assertEquals(testSeason, result);
            verify(queryService).getSeasonById(1);
        }

        @Test
        @DisplayName("ArchiveSeason() delegates to StatusService")
        void archiveSeason_DelegatesToStatusService() {
            // Arrange
            when(statusService.archiveSeason(1)).thenReturn(null);

            // Act
            seasonService.ArchiveSeason(1);

            // Assert
            verify(statusService).archiveSeason(1);
        }

        @Test
        @DisplayName("ValidateStatusConstraints() delegates to StatusService")
        void validateStatusConstraints_DelegatesToStatusService() {
            // Arrange
            when(statusService.validateStatusConstraints(SeasonStatus.PLANNED, SeasonStatus.ACTIVE))
                    .thenReturn(true);

            // Act
            boolean result = seasonService.ValidateStatusConstraints(SeasonStatus.PLANNED, SeasonStatus.ACTIVE);

            // Assert
            assertTrue(result);
            verify(statusService).validateStatusConstraints(SeasonStatus.PLANNED, SeasonStatus.ACTIVE);
        }
    }
}

