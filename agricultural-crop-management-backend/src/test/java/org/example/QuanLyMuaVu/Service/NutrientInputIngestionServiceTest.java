package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateNutrientInputRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.NutrientInputResponse;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.example.QuanLyMuaVu.module.sustainability.service.NutrientInputIngestionService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutrientInputIngestionServiceTest {

    @Mock
    private NutrientInputEventRepository nutrientInputEventRepository;
    @Mock
    private FarmerOwnershipService ownershipService;
    @Mock
    private CurrentUserService currentUserService;

    private NutrientInputIngestionService service;
    private Plot plot;
    private Season season;

    @BeforeEach
    void setUp() {
        service = new NutrientInputIngestionService(
                nutrientInputEventRepository,
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
    @DisplayName("Create nutrient input keeps measured source metadata and traceability")
    void create_WhenMeasuredSource_ShouldStoreTraceableMeasuredEvent() {
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(22)
                .inputSource(NutrientInputSource.MINERAL_FERTILIZER)
                .value(new BigDecimal("45"))
                .unit("kg_n")
                .recordedAt(LocalDate.of(2026, 3, 1))
                .sourceType(NutrientInputSourceType.LAB_MEASURED)
                .sourceDocument("lab-report-2026-03-01.pdf")
                .note("Field sample from lab A")
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(nutrientInputEventRepository.save(any(NutrientInputEvent.class)))
                .thenAnswer(invocation -> {
                    NutrientInputEvent event = invocation.getArgument(0, NutrientInputEvent.class);
                    event.setId(1001);
                    event.setCreatedAt(LocalDateTime.of(2026, 3, 1, 8, 30));
                    return event;
                });

        NutrientInputResponse response = service.create(33, request);

        ArgumentCaptor<NutrientInputEvent> captor = ArgumentCaptor.forClass(NutrientInputEvent.class);
        verify(nutrientInputEventRepository).save(captor.capture());
        NutrientInputEvent saved = captor.getValue();

        assertEquals(new BigDecimal("45.0000"), saved.getNKg());
        assertTrue(saved.getMeasured());
        assertEquals(NutrientInputSourceType.LAB_MEASURED, saved.getSourceType());
        assertEquals("lab-report-2026-03-01.pdf", saved.getSourceDocument());
        assertEquals(99L, saved.getCreatedByUserId());

        assertEquals("measured", response.getStatus());
        assertEquals(new BigDecimal("45.0000"), response.getNormalizedNKg());
        assertEquals(NutrientInputSourceType.LAB_MEASURED, response.getSourceType());
    }

    @Test
    @DisplayName("Create nutrient input converts kg_n_per_ha to normalized kg N by plot area")
    void create_WhenInputUnitPerHa_ShouldNormalizeUsingPlotArea() {
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(22)
                .inputSource(NutrientInputSource.ORGANIC_FERTILIZER)
                .value(new BigDecimal("12"))
                .unit("kg_n_per_ha")
                .recordedAt(LocalDate.of(2026, 3, 2))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);
        when(currentUserService.getCurrentUserId()).thenReturn(77L);
        when(nutrientInputEventRepository.save(any(NutrientInputEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, NutrientInputEvent.class));

        NutrientInputResponse response = service.create(33, request);

        assertEquals(new BigDecimal("24.0000"), response.getNormalizedNKg());
        assertEquals("measured", response.getStatus());
    }

    @Test
    @DisplayName("List nutrient inputs keeps estimated semantic for non-measured source")
    void list_WhenSourceIsEstimated_ShouldReturnEstimatedStatus() {
        NutrientInputEvent event = NutrientInputEvent.builder()
                .id(7)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("4.5000"))
                .appliedDate(LocalDate.of(2026, 3, 4))
                .measured(false)
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .createdByUserId(11L)
                .createdAt(LocalDateTime.of(2026, 3, 4, 9, 0))
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(nutrientInputEventRepository.findAllBySeasonId(33)).thenReturn(List.of(event));

        List<NutrientInputResponse> responses = service.list(33, null, NutrientInputSource.IRRIGATION_WATER);

        assertEquals(1, responses.size());
        assertEquals("estimated", responses.get(0).getStatus());
        assertEquals(NutrientInputSourceType.SYSTEM_ESTIMATED, responses.get(0).getSourceType());
        assertEquals(new BigDecimal("4.5000"), responses.get(0).getNormalizedNKg());
    }

    @Test
    @DisplayName("Create nutrient input rejects season-plot mismatch")
    void create_WhenSeasonAndPlotMismatch_ShouldThrowBadRequest() {
        Plot otherPlot = Plot.builder().id(999).plotName("Other").area(new BigDecimal("1.0")).build();
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(999)
                .inputSource(NutrientInputSource.MINERAL_FERTILIZER)
                .value(BigDecimal.ONE)
                .unit("kg_n")
                .recordedAt(LocalDate.now())
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(999)).thenReturn(otherPlot);

        assertThrows(AppException.class, () -> service.create(33, request));
    }

    @Test
    @DisplayName("Create nutrient input rejects deprecated legacy aggregate sources")
    void create_WhenLegacyAggregateSourceProvided_ShouldThrowDeprecatedError() {
        CreateNutrientInputRequest request = CreateNutrientInputRequest.builder()
                .plotId(22)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .value(new BigDecimal("5"))
                .unit("kg_n")
                .recordedAt(LocalDate.now())
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(ownershipService.requireOwnedSeason(33)).thenReturn(season);
        when(ownershipService.requireOwnedPlot(22)).thenReturn(plot);

        AppException exception = assertThrows(AppException.class, () -> service.create(33, request));
        assertEquals(ErrorCode.LEGACY_NUTRIENT_INPUT_DEPRECATED, exception.getErrorCode());
        verify(nutrientInputEventRepository, never()).save(any(NutrientInputEvent.class));
    }
}
