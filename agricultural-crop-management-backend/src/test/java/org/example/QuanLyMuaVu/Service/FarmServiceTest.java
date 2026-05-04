package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.dto.request.FarmCreateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmUpdateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.example.QuanLyMuaVu.module.farm.service.FarmService;
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
 * Functional tests for FarmService.
 * 
 * Covers key operations: create, update, delete, get farms.
 */
@ExtendWith(MockitoExtension.class)
public class FarmServiceTest {

        @Mock
        private FarmRepository farmRepository;

        @Mock
        private ProvinceRepository provinceRepository;

        @Mock
        private WardRepository wardRepository;

        @Mock
        private CurrentUserService currentUserService;

        @InjectMocks
        private FarmService farmService;

        private User testUser;
        private Province testProvince;
        private Ward testWard;
        private Farm testFarm;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .username("farmer")
                                .email("farmer@test.com")
                                .build();

                testProvince = Province.builder()
                                .id(1)
                                .name("Test Province")
                                .build();

                testWard = Ward.builder()
                                .id(1)
                                .name("Test Ward")
                                .province(testProvince)
                                .build();

                testFarm = Farm.builder()
                                .id(1)
                                .name("Test Farm")
                                .user(testUser)
                                .province(testProvince)
                                .ward(testWard)
                                .area(BigDecimal.valueOf(100))
                                .active(true)
                                .build();
        }

        @Test
        @DisplayName("CreateFarm - Creates farm with valid data")
        void createFarm_WithValidData_ReturnsFarmResponse() {
                // Arrange
                FarmCreateRequest request = FarmCreateRequest.builder()
                                .farmName("New Farm")
                                .provinceId(1)
                                .wardId(1)
                                .area(BigDecimal.valueOf(50))
                                .active(true)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.existsByUserAndNameIgnoreCaseAndActiveTrue(testUser, "New Farm")).thenReturn(false);
                when(provinceRepository.findById(1)).thenReturn(Optional.of(testProvince));
                when(wardRepository.findById(1)).thenReturn(Optional.of(testWard));
                when(farmRepository.save(any())).thenAnswer(i -> {
                        Farm f = i.getArgument(0);
                        f.setId(1);
                        return f;
                });

                // Act
                FarmResponse response = farmService.createFarm(request);

                // Assert
                assertNotNull(response);
                assertEquals("New Farm", response.getFarmName());
                verify(farmRepository).save(any());
        }

        @Test
        @DisplayName("CreateFarm - Throws FARM_NAME_EXISTS when name is duplicate")
        void createFarm_WhenNameExists_ThrowsAppException() {
                // Arrange
                FarmCreateRequest request = FarmCreateRequest.builder()
                                .farmName("Existing Farm")
                                .provinceId(1)
                                .wardId(1)
                                .area(BigDecimal.valueOf(50))
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.existsByUserAndNameIgnoreCaseAndActiveTrue(testUser, "Existing Farm"))
                                .thenReturn(true);

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> farmService.createFarm(request));

                assertEquals(ErrorCode.FARM_NAME_EXISTS, exception.getErrorCode());
                verify(farmRepository, never()).save(any());
        }

        @Test
        @DisplayName("CreateFarm - Throws WARD_NOT_IN_PROVINCE when ward doesn't belong to province")
        void createFarm_WhenWardNotInProvince_ThrowsAppException() {
                // Arrange
                Province differentProvince = Province.builder().id(2).name("Different Province").build();
                Ward wardInDifferentProvince = Ward.builder()
                                .id(2)
                                .name("Other Ward")
                                .province(differentProvince)
                                .build();

                FarmCreateRequest request = FarmCreateRequest.builder()
                                .farmName("New Farm")
                                .provinceId(1) // Province 1
                                .wardId(2) // Ward in Province 2
                                .area(BigDecimal.valueOf(50))
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.existsByUserAndNameIgnoreCaseAndActiveTrue(any(), anyString())).thenReturn(false);
                when(provinceRepository.findById(1)).thenReturn(Optional.of(testProvince));
                when(wardRepository.findById(2)).thenReturn(Optional.of(wardInDifferentProvince));

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> farmService.createFarm(request));

                assertEquals(ErrorCode.WARD_NOT_IN_PROVINCE, exception.getErrorCode());
        }

        @Test
        @DisplayName("DeleteFarm - Throws FARM_HAS_CHILD_RECORDS when farm has plots or seasons")
        void deleteFarm_WhenHasChildRecords_ThrowsAppException() {
                // Arrange
                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(testFarm));
                when(farmRepository.hasPlots(1)).thenReturn(true); // Has plots

                // Act & Assert
                AppException exception = assertThrows(AppException.class,
                                () -> farmService.deleteFarm(1));

                assertEquals(ErrorCode.FARM_HAS_CHILD_RECORDS, exception.getErrorCode());
        }

        @Test
        @DisplayName("GetFarmDetail - Throws AccessDeniedException when user doesn't own farm")
        void getFarmDetail_WhenNotOwner_ThrowsAccessDeniedException() {
                // Arrange
                when(currentUserService.getCurrentUser()).thenReturn(testUser);
                when(farmRepository.findByIdAndUser(999, testUser)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(AccessDeniedException.class,
                                () -> farmService.getFarmDetail(999));
        }
}
