package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateSoilTestRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SoilTestResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.example.QuanLyMuaVu.module.sustainability.service.SoilTestService;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoilTestServiceTest {

    @Mock
    private SoilTestRepository soilTestRepository;
    @Mock
    private FarmerOwnershipService ownershipService;
    @Mock
    private CurrentUserService currentUserService;

    private SoilTestService service;
    private Plot plot;
    private Season season;

    @BeforeEach
    void setUp() {
        service = new SoilTestService(
                soilTestRepository,
                ownershipService,
                currentUserService
        );

        plot = Plot.builder()
                .id(22)
                .plotName("Plot A")
                .area(new BigDecimal("2.00"))
                .build();

        season = Season.builder()
                .id(33)
                .seasonName("Season 33")
                .plot(plot)
                .status(SeasonStatus.PLANNED)
                .build();
    }

    @Test
    @DisplayName("Create soil-test computes N contribution from mineral N per ha and area")
    void create_WhenAreaAvailable_ComputesContribution() {
        CreateSoilTestRequest request = CreateSoilTestRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 20))
                .soilOrganicMatterPct(new BigDecimal("2.30"))
                .mineralNKgPerHa(new BigDecimal("15.50"))
                .nitrateMgPerKg(new BigDecimal("12.10"))
                .ammoniumMgPerKg(new BigDecimal("2.20"))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("soil-lab.pdf")
                .labReference("LAB-SOIL-11")
                .note("Lab tested")
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(soilTestRepository.save(any(SoilTest.class)))
                .thenAnswer(invocation -> {
                    SoilTest test = invocation.getArgument(0, SoilTest.class);
                    test.setId(4001);
                    test.setCreatedAt(LocalDateTime.of(2026, 3, 20, 9, 15));
                    return test;
                });

        SoilTestResponse response = service.create(33, request);

        ArgumentCaptor<SoilTest> captor = ArgumentCaptor.forClass(SoilTest.class);
        verify(soilTestRepository).save(captor.capture());
        SoilTest saved = captor.getValue();

        assertEquals(new BigDecimal("15.5000"), saved.getMineralNKgPerHa());
        assertEquals(NutrientInputSourceType.LAB_MEASURED, saved.getSourceType());
        assertEquals(new BigDecimal("31.0000"), response.getEstimatedNContributionKg());
        assertEquals("measured", response.getStatus());
    }

    @Test
    @DisplayName("Create soil-test keeps contribution null when area is unavailable")
    void create_WhenAreaMissing_ReturnsNullContribution() {
        Plot areaMissingPlot = Plot.builder()
                .id(22)
                .plotName("Plot A")
                .area(null)
                .build();
        season.setPlot(areaMissingPlot);

        CreateSoilTestRequest request = CreateSoilTestRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 20))
                .mineralNKgPerHa(new BigDecimal("8.00"))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(areaMissingPlot);
        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(soilTestRepository.save(any(SoilTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, SoilTest.class));

        SoilTestResponse response = service.create(33, request);

        assertNull(response.getEstimatedNContributionKg());
        assertEquals("measured", response.getStatus());
    }

    @Test
    @DisplayName("List soil-tests preserves estimated status for non-measured source type")
    void list_WhenSourceTypeEstimated_ReturnsEstimatedStatus() {
        SoilTest estimated = SoilTest.builder()
                .id(31)
                .season(season)
                .plot(plot)
                .sampleDate(LocalDate.of(2026, 3, 10))
                .mineralNKgPerHa(new BigDecimal("7.5000"))
                .measured(false)
                .sourceType(NutrientInputSourceType.EXTERNAL_REFERENCE)
                .createdAt(LocalDateTime.of(2026, 3, 10, 7, 30))
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(soilTestRepository.findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(33))
                .thenReturn(List.of(estimated));

        List<SoilTestResponse> result = service.list(33, null);

        assertEquals(1, result.size());
        assertEquals("estimated", result.get(0).getStatus());
        assertEquals(new BigDecimal("15.0000"), result.get(0).getEstimatedNContributionKg());
    }
}
