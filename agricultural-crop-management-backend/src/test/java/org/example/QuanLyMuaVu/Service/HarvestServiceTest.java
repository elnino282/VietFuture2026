package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.season.dto.request.HarvestRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.HarvestMapper;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.service.HarvestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for HarvestService.
 * 
 * Covers key operations: create, update, delete, get all harvests.
 */
@ExtendWith(MockitoExtension.class)
public class HarvestServiceTest {

    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private HarvestMapper harvestMapper;

    @InjectMocks
    private HarvestService harvestService;

    private Season testSeason;
    private Harvest testHarvest;
    private HarvestResponse harvestResponse;

    @BeforeEach
    void setUp() {
        testSeason = Season.builder()
                .id(1)
                .seasonName("Spring 2024")
                .build();

        testHarvest = Harvest.builder()
                .id(1)
                .season(testSeason)
                .harvestDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(500))
                .unit(BigDecimal.valueOf(1))
                .build();

        harvestResponse = HarvestResponse.builder()
                .id(1)
                .seasonId(1)
                .seasonName("Spring 2024")
                .harvestDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(500))
                .unit(BigDecimal.valueOf(1))
                .build();
    }

    @Test
    @DisplayName("Create - Creates harvest with valid data")
    void create_WithValidData_ReturnsHarvestResponse() {
        // Arrange
        HarvestRequest request = HarvestRequest.builder()
                .seasonId(1)
                .harvestDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(500))
                .unit(BigDecimal.valueOf(1))
                .build();

        when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));
        doNothing().when(harvestMapper).update(any(Harvest.class), eq(request));
        when(harvestRepository.save(any())).thenAnswer(i -> {
            Harvest h = i.getArgument(0);
            h.setId(1);
            return h;
        });
        when(harvestMapper.toResponse(any())).thenReturn(harvestResponse);

        // Act
        HarvestResponse response = harvestService.create(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSeasonId());
        assertEquals(BigDecimal.valueOf(500), response.getQuantity());
        verify(harvestRepository).save(any());
    }

    @Test
    @DisplayName("Create - Throws NoSuchElementException when season not found")
    void create_WhenSeasonNotFound_ThrowsException() {
        // Arrange
        HarvestRequest request = HarvestRequest.builder()
                .seasonId(999)
                .harvestDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(100))
                .build();

        when(seasonRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class,
                () -> harvestService.create(request));

        verify(harvestRepository, never()).save(any());
    }

    @Test
    @DisplayName("GetAll - Returns all harvests")
    void getAll_ReturnsAllHarvests() {
        // Arrange
        Harvest harvest2 = Harvest.builder()
                .id(2)
                .season(testSeason)
                .quantity(BigDecimal.valueOf(300))
                .build();

        HarvestResponse response2 = HarvestResponse.builder()
                .id(2)
                .seasonId(1)
                .quantity(BigDecimal.valueOf(300))
                .build();

        when(harvestRepository.findAll()).thenReturn(List.of(testHarvest, harvest2));
        when(harvestMapper.toResponse(testHarvest)).thenReturn(harvestResponse);
        when(harvestMapper.toResponse(harvest2)).thenReturn(response2);

        // Act
        List<HarvestResponse> result = harvestService.getAll();

        // Assert
        assertEquals(2, result.size());
        verify(harvestRepository).findAll();
    }

    @Test
    @DisplayName("Update - Updates harvest successfully")
    void update_WithValidData_UpdatesHarvest() {
        // Arrange
        HarvestRequest updateRequest = HarvestRequest.builder()
                .seasonId(1)
                .harvestDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(600))
                .unit(BigDecimal.valueOf(2))
                .build();

        when(harvestRepository.findById(1)).thenReturn(Optional.of(testHarvest));
        doNothing().when(harvestMapper).update(testHarvest, updateRequest);
        when(harvestRepository.save(testHarvest)).thenReturn(testHarvest);
        when(harvestMapper.toResponse(testHarvest)).thenReturn(harvestResponse);

        // Act
        HarvestResponse response = harvestService.update(1, updateRequest);

        // Assert
        assertNotNull(response);
        verify(harvestMapper).update(testHarvest, updateRequest);
        verify(harvestRepository).save(testHarvest);
    }

    @Test
    @DisplayName("Delete - Deletes harvest by ID")
    void delete_WithValidId_DeletesHarvest() {
        // Arrange
        doNothing().when(harvestRepository).deleteById(1);

        // Act
        assertDoesNotThrow(() -> harvestService.delete(1));

        // Assert
        verify(harvestRepository).deleteById(1);
    }
}
