package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateIrrigationWaterAnalysisRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.IrrigationWaterAnalysisResponse;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.module.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.example.QuanLyMuaVu.module.sustainability.service.IrrigationWaterAnalysisService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrrigationWaterAnalysisServiceTest {

    @Mock
    private IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    @Mock
    private FarmerOwnershipService ownershipService;
    @Mock
    private CurrentUserService currentUserService;

    private IrrigationWaterAnalysisService service;
    private Plot plot;
    private Season season;

    @BeforeEach
    void setUp() {
        service = new IrrigationWaterAnalysisService(
                irrigationWaterAnalysisRepository,
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
    @DisplayName("Create irrigation analysis computes N contribution from nitrate+ammonium and stores traceability")
    void create_WhenNitrateAndAmmoniumProvided_ComputesContribution() {
        CreateIrrigationWaterAnalysisRequest request = CreateIrrigationWaterAnalysisRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .nitrateMgPerL(new BigDecimal("4.2"))
                .ammoniumMgPerL(new BigDecimal("1.8"))
                .irrigationVolumeM3(new BigDecimal("500"))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("water-lab.pdf")
                .labReference("LAB-IRR-101")
                .note("Canal sample")
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(irrigationWaterAnalysisRepository.save(any(IrrigationWaterAnalysis.class)))
                .thenAnswer(invocation -> {
                    IrrigationWaterAnalysis item = invocation.getArgument(0, IrrigationWaterAnalysis.class);
                    item.setId(1001);
                    item.setCreatedAt(LocalDateTime.of(2026, 3, 18, 9, 0));
                    return item;
                });

        IrrigationWaterAnalysisResponse response = service.create(33, request);

        ArgumentCaptor<IrrigationWaterAnalysis> captor = ArgumentCaptor.forClass(IrrigationWaterAnalysis.class);
        verify(irrigationWaterAnalysisRepository).save(captor.capture());
        IrrigationWaterAnalysis saved = captor.getValue();

        assertEquals(new BigDecimal("4.2000"), saved.getNitrateMgPerL());
        assertEquals(new BigDecimal("1.8000"), saved.getAmmoniumMgPerL());
        assertEquals(new BigDecimal("500.0000"), saved.getIrrigationVolumeM3());
        assertEquals(NutrientInputSourceType.LAB_MEASURED, saved.getSourceType());

        assertEquals(new BigDecimal("6.0000"), response.getEffectiveNmgPerL());
        assertEquals(new BigDecimal("3.0000"), response.getEstimatedNContributionKg());
        assertEquals("measured", response.getStatus());
    }

    @Test
    @DisplayName("Create irrigation analysis prefers totalN when available")
    void create_WhenTotalNProvided_UsesTotalNForContribution() {
        CreateIrrigationWaterAnalysisRequest request = CreateIrrigationWaterAnalysisRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .totalNmgPerL(new BigDecimal("8"))
                .irrigationVolumeM3(new BigDecimal("250"))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        when(irrigationWaterAnalysisRepository.save(any(IrrigationWaterAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, IrrigationWaterAnalysis.class));

        IrrigationWaterAnalysisResponse response = service.create(33, request);

        assertEquals(new BigDecimal("8.0000"), response.getEffectiveNmgPerL());
        assertEquals(new BigDecimal("2.0000"), response.getEstimatedNContributionKg());
        assertEquals("measured", response.getStatus());
    }

    @Test
    @DisplayName("Create irrigation analysis rejects request when all concentration fields are missing")
    void create_WhenConcentrationMissing_ShouldThrowBadRequest() {
        CreateIrrigationWaterAnalysisRequest request = CreateIrrigationWaterAnalysisRequest.builder()
                .plotId(22)
                .sampleDate(LocalDate.of(2026, 3, 18))
                .irrigationVolumeM3(new BigDecimal("100"))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);

        assertThrows(AppException.class, () -> service.create(33, request));
    }

    @Test
    @DisplayName("List irrigation analyses preserves estimated status")
    void list_WhenSourceTypeEstimated_ReturnsEstimatedStatus() {
        IrrigationWaterAnalysis estimated = IrrigationWaterAnalysis.builder()
                .id(12)
                .season(season)
                .plot(plot)
                .sampleDate(LocalDate.of(2026, 3, 17))
                .totalNmgPerL(new BigDecimal("5.0000"))
                .irrigationVolumeM3(new BigDecimal("300.0000"))
                .measured(false)
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .createdAt(LocalDateTime.of(2026, 3, 17, 8, 30))
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(irrigationWaterAnalysisRepository.findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(33))
                .thenReturn(List.of(estimated));

        List<IrrigationWaterAnalysisResponse> result = service.list(33, null);

        assertEquals(1, result.size());
        assertEquals("estimated", result.get(0).getStatus());
        assertEquals(new BigDecimal("1.5000"), result.get(0).getEstimatedNContributionKg());
    }
}
