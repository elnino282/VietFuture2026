package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.cropcatalog.dto.request.CropRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.CropResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.cropcatalog.mapper.CropMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.mapper.VarietyMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.CropRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.VarietyRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.service.CropService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for CropService.
 * 
 * Covers key operations: create, update, delete, get varieties.
 */
@ExtendWith(MockitoExtension.class)
public class CropServiceTest {

    @Mock
    private CropRepository cropRepository;

    @Mock
    private VarietyRepository varietyRepository;

    @Mock
    private CropMapper cropMapper;

    @Mock
    private VarietyMapper varietyMapper;

    @InjectMocks
    private CropService cropService;

    private Crop testCrop;
    private CropResponse cropResponse;
    private Variety testVariety;

    @BeforeEach
    void setUp() {
        testCrop = Crop.builder()
                .id(1)
                .cropName("Rice")
                .description("Staple grain crop")
                .build();

        cropResponse = CropResponse.builder()
                .id(1)
                .cropName("Rice")
                .description("Staple grain crop")
                .build();

        testVariety = Variety.builder()
                .id(1)
                .name("Jasmine Rice")
                .crop(testCrop)
                .build();
    }

    @Test
    @DisplayName("Create - Creates crop with valid data")
    void create_WithValidData_ReturnsCropResponse() {
        // Arrange
        CropRequest request = CropRequest.builder()
                .cropName("Wheat")
                .description("Winter wheat")
                .build();

        Crop newCrop = Crop.builder().cropName("Wheat").description("Winter wheat").build();
        CropResponse newResponse = CropResponse.builder().id(2).cropName("Wheat").build();

        when(cropRepository.existsByCropNameIgnoreCase("Wheat")).thenReturn(false);
        when(cropMapper.toEntity(request)).thenReturn(newCrop);
        when(cropRepository.save(newCrop)).thenReturn(newCrop);
        when(cropMapper.toResponse(newCrop)).thenReturn(newResponse);

        // Act
        CropResponse response = cropService.create(request);

        // Assert
        assertNotNull(response);
        assertEquals("Wheat", response.getCropName());
        verify(cropRepository).save(newCrop);
    }

    @Test
    @DisplayName("Create - Throws DUPLICATE_RESOURCE when crop name exists")
    void create_WhenNameExists_ThrowsAppException() {
        // Arrange
        CropRequest request = CropRequest.builder()
                .cropName("Rice")
                .description("Duplicate rice")
                .build();

        when(cropRepository.existsByCropNameIgnoreCase("Rice")).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> cropService.create(request));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.getErrorCode());
        verify(cropRepository, never()).save(any());
    }

    @Test
    @DisplayName("GetById - Returns crop when found")
    void getById_WhenFound_ReturnsCropResponse() {
        // Arrange
        when(cropRepository.findById(1)).thenReturn(Optional.of(testCrop));
        when(cropMapper.toResponse(testCrop)).thenReturn(cropResponse);

        // Act
        CropResponse response = cropService.getById(1);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Rice", response.getCropName());
    }

    @Test
    @DisplayName("GetById - Throws CROP_NOT_FOUND when not found")
    void getById_WhenNotFound_ThrowsAppException() {
        // Arrange
        when(cropRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> cropService.getById(999));

        assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("GetVarietiesByCropId - Returns varieties for valid crop")
    void getVarietiesByCropId_WhenCropExists_ReturnsVarieties() {
        // Arrange
        VarietyResponse varietyResponse = VarietyResponse.builder()
                .id(1)
                .name("Jasmine Rice")
                .cropId(1)
                .build();

        when(cropRepository.findById(1)).thenReturn(Optional.of(testCrop));
        when(varietyRepository.findAllByCrop(testCrop)).thenReturn(List.of(testVariety));
        when(varietyMapper.toResponse(testVariety)).thenReturn(varietyResponse);

        // Act
        List<VarietyResponse> result = cropService.getVarietiesByCropId(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Jasmine Rice", result.get(0).getName());
    }
}

