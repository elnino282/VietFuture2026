package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.dto.request.PlotRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.PlotResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.PlotStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.farm.repository.PlotRepository;
import org.example.QuanLyMuaVu.module.farm.service.PlotService;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for PlotService.
 * 
 * Covers key operations: create, update, delete, list plots.
 */
@ExtendWith(MockitoExtension.class)
public class PlotServiceTest {

        @Mock
        private PlotRepository plotRepository;

        @Mock
        private FarmRepository farmRepository;

        @Mock
        private CurrentUserService currentUserService;

        @InjectMocks
        private PlotService plotService;

        private User testUser;
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
                                .area(BigDecimal.valueOf(100))
                                .active(true)
                                .build();

                testPlot = Plot.builder()
                                .id(1)
                                .plotName("Test Plot")
                                .farm(testFarm)
                                .user(testUser)
                                .area(BigDecimal.valueOf(10))
                                .soilType("Loam")
                                .status(PlotStatus.IDLE.getCode())
                                .build();
        }

        @Test
        @DisplayName("CreatePlotForFarm - Creates plot with valid data")
        void createPlotForFarm_WithValidData_ReturnsPlotResponse() {
                // Arrange
                PlotRequest request = PlotRequest.builder()
                                .plotName("New Plot")
                                .area(BigDecimal.valueOf(5))
                                .soilType("Clay")
                                .status(PlotStatus.IDLE)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(testFarm));
                when(plotRepository.save(any())).thenAnswer(i -> {
                        Plot p = i.getArgument(0);
                        p.setId(1);
                        return p;
                });

                // Act
                PlotResponse response = plotService.createPlotForFarm(1, request);

                // Assert
                assertNotNull(response);
                assertEquals("New Plot", response.getPlotName());
                assertEquals(PlotStatus.IDLE, response.getStatus());
                verify(plotRepository).save(any());
        }

        @Test
        @DisplayName("CreatePlotForFarm - Throws AccessDeniedException when user doesn't own farm")
        void createPlotForFarm_WhenNotOwner_ThrowsAccessDeniedException() {
                // Arrange
                PlotRequest request = PlotRequest.builder()
                                .plotName("New Plot")
                                .area(BigDecimal.valueOf(5))
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(999, testUser)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(AccessDeniedException.class,
                                () -> plotService.createPlotForFarm(999, request));

                verify(plotRepository, never()).save(any());
        }

        @Test
        @DisplayName("CreatePlotForFarm - Throws FARM_INACTIVE when farm is inactive")
        void createPlotForFarm_WhenFarmInactive_ThrowsAppException() {
                // Arrange
                Farm inactiveFarm = Farm.builder()
                                .id(2)
                                .name("Inactive Farm")
                                .user(testUser)
                                .active(false)
                                .build();

                PlotRequest request = PlotRequest.builder()
                                .plotName("New Plot")
                                .area(BigDecimal.valueOf(5))
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(2, testUser)).thenReturn(Optional.of(inactiveFarm));

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> plotService.createPlotForFarm(2, request));

                assertEquals(ErrorCode.FARM_INACTIVE, exception.getErrorCode());
        }

        @Test
        @DisplayName("ListPlotsByFarm - Returns plots for farm owner")
        void listPlotsByFarm_ForOwner_ReturnsPlotList() {
                // Arrange
                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(testFarm));
                when(plotRepository.findAllByFarm(testFarm)).thenReturn(List.of(testPlot));

                // Act
                List<PlotResponse> result = plotService.listPlotsByFarm(1);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("Test Plot", result.get(0).getPlotName());
        }

        @Test
        @DisplayName("CreatePlotForCurrentFarmer - Throws BAD_REQUEST when farmId is null")
        void createPlotForCurrentFarmer_WithNullFarmId_ThrowsAppException() {
                // Arrange
                PlotRequest request = PlotRequest.builder()
                                .farmId(null) // No farm specified
                                .plotName("New Plot")
                                .area(BigDecimal.valueOf(5))
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> plotService.createPlotForCurrentFarmer(request));

                assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        }
}
