package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.service.FarmAccessService;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.FieldLogResponse;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.service.FieldLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for FieldLogService.
 * 
 * Covers key operations: create, update, delete, season status validation.
 */
@ExtendWith(MockitoExtension.class)
public class FieldLogServiceTest {

        @Mock
        private FieldLogRepository fieldLogRepository;

        @Mock
        private SeasonRepository seasonRepository;

        @Mock
        private FarmAccessService farmAccessService;

        @InjectMocks
        private FieldLogService fieldLogService;

        private Season testSeason;
        private FieldLog testFieldLog;
        private Plot testPlot;

        @BeforeEach
        void setUp() {
                testPlot = Plot.builder()
                                .id(1)
                                .plotName("Test Plot")
                                .build();

                testSeason = Season.builder()
                                .id(1)
                                .seasonName("Spring 2024")
                                .plot(testPlot)
                                .status(SeasonStatus.ACTIVE)
                                .startDate(LocalDate.now().minusMonths(1))
                                .endDate(LocalDate.now().plusMonths(2))
                                .build();

                testFieldLog = FieldLog.builder()
                                .id(1)
                                .season(testSeason)
                                .logDate(LocalDate.now())
                                .logType("GROWTH")
                                .notes("Plants looking healthy")
                                .build();
        }

        @Test
        @DisplayName("CreateFieldLog - Creates field log with valid data")
        void createFieldLog_WithValidData_ReturnsFieldLogResponse() {
                // Arrange
                CreateFieldLogRequest request = CreateFieldLogRequest.builder()
                                .logDate(LocalDate.now())
                                .logType("GROWTH")
                                .notes("Test observation")
                                .build();

                when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
                doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(testSeason);
                when(fieldLogRepository.save(any())).thenAnswer(i -> {
                        FieldLog log = i.getArgument(0);
                        log.setId(1);
                        return log;
                });

                // Act
                FieldLogResponse response = fieldLogService.createFieldLog(1, request);

                // Assert
                assertNotNull(response);
                assertEquals("GROWTH", response.getLogType());
                verify(fieldLogRepository).save(any());
        }

        @Test
        @DisplayName("CreateFieldLog - Throws error when season is COMPLETED")
        void createFieldLog_WhenSeasonCompleted_ThrowsAppException() {
                // Arrange
                Season completedSeason = Season.builder()
                                .id(2)
                                .seasonName("Completed Season")
                                .status(SeasonStatus.COMPLETED)
                                .startDate(LocalDate.now().minusMonths(3))
                                .endDate(LocalDate.now().minusMonths(1))
                                .build();

                CreateFieldLogRequest request = CreateFieldLogRequest.builder()
                                .logDate(LocalDate.now())
                                .logType("GROWTH")
                                .build();

                when(seasonRepository.findById(2)).thenReturn(Optional.of(completedSeason));
                doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> fieldLogService.createFieldLog(2, request));

                assertEquals(ErrorCode.SEASON_CLOSED_CANNOT_ADD_FIELD_LOG, exception.getErrorCode());
                verify(fieldLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("CreateFieldLog - Throws INVALID_LOG_TYPE for invalid log type")
        void createFieldLog_WithInvalidLogType_ThrowsAppException() {
                // Arrange
                CreateFieldLogRequest request = CreateFieldLogRequest.builder()
                                .logDate(LocalDate.now())
                                .logType("INVALID_TYPE") // Not a valid log type
                                .build();

                when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
                doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> fieldLogService.createFieldLog(1, request));

                assertEquals(ErrorCode.INVALID_LOG_TYPE, exception.getErrorCode());
        }

        @Test
        @DisplayName("CreateFieldLog - Throws error when log date is outside season dates")
        void createFieldLog_WhenDateOutsideSeasonRange_ThrowsAppException() {
                // Arrange
                CreateFieldLogRequest request = CreateFieldLogRequest.builder()
                                .logDate(LocalDate.now().minusYears(1)) // Date before season started
                                .logType("GROWTH") // Use valid log type
                                .build();

                when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
                doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> fieldLogService.createFieldLog(1, request));

                assertEquals(ErrorCode.INVALID_SEASON_DATES, exception.getErrorCode());
        }

        @Test
        @DisplayName("DeleteFieldLog - Throws error when trying to delete from CANCELLED season")
        void deleteFieldLog_WhenSeasonCancelled_ThrowsAppException() {
                // Arrange
                Season cancelledSeason = Season.builder()
                                .id(2)
                                .seasonName("Cancelled Season")
                                .status(SeasonStatus.CANCELLED)
                                .build();

                FieldLog logInCancelledSeason = FieldLog.builder()
                                .id(2)
                                .season(cancelledSeason)
                                .logDate(LocalDate.now())
                                .logType("GROWTH")
                                .build();

                when(fieldLogRepository.findById(2)).thenReturn(Optional.of(logInCancelledSeason));
                doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(any());

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> fieldLogService.deleteFieldLog(2));

                assertEquals(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_FIELD_LOG, exception.getErrorCode());
                verify(fieldLogRepository, never()).delete(any());
        }
}
