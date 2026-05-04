package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.module.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.example.QuanLyMuaVu.module.sustainability.service.LegacyNutrientInputBackfillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyNutrientInputBackfillServiceTest {

    @Mock
    private NutrientInputEventRepository nutrientInputEventRepository;
    @Mock
    private IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    @Mock
    private SoilTestRepository soilTestRepository;

    private LegacyNutrientInputBackfillService service;
    private Season season;
    private Plot plot;

    @BeforeEach
    void setUp() {
        service = new LegacyNutrientInputBackfillService(
                nutrientInputEventRepository,
                irrigationWaterAnalysisRepository,
                soilTestRepository
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
                .build();
    }

    @Test
    @DisplayName("Backfill migrates legacy irrigation and soil records with traceability")
    void backfill_WhenLegacyEventsExist_ShouldMigrateToDedicatedDomains() {
        NutrientInputEvent irrigationEvent = NutrientInputEvent.builder()
                .id(1001)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("5.4000"))
                .appliedDate(LocalDate.of(2026, 3, 10))
                .measured(true)
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .sourceDocument("legacy-irrigation.csv")
                .note("legacy irrigation")
                .createdByUserId(7L)
                .createdAt(LocalDateTime.of(2026, 3, 10, 8, 0))
                .build();

        NutrientInputEvent soilEvent = NutrientInputEvent.builder()
                .id(1002)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.SOIL_LEGACY)
                .nKg(new BigDecimal("6.0000"))
                .appliedDate(LocalDate.of(2026, 3, 11))
                .measured(false)
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .sourceDocument("legacy-soil.csv")
                .note("legacy soil")
                .createdByUserId(8L)
                .createdAt(LocalDateTime.of(2026, 3, 11, 9, 0))
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(any()))
                .thenReturn(List.of(irrigationEvent, soilEvent));
        when(irrigationWaterAnalysisRepository.existsByLegacyEventId(1001)).thenReturn(false);
        when(soilTestRepository.existsByLegacyEventId(1002)).thenReturn(false);
        when(irrigationWaterAnalysisRepository.save(any(IrrigationWaterAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, IrrigationWaterAnalysis.class));
        when(soilTestRepository.save(any(SoilTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, SoilTest.class));

        LegacyNutrientInputBackfillService.BackfillReport report = service.backfillLegacyAggregateInputs();

        assertEquals(2, report.getScannedCount());
        assertEquals(1, report.getMigratedIrrigationCount());
        assertEquals(1, report.getMigratedSoilCount());
        assertEquals(0, report.getSkippedAlreadyMigratedCount());

        ArgumentCaptor<IrrigationWaterAnalysis> irrigationCaptor = ArgumentCaptor.forClass(IrrigationWaterAnalysis.class);
        verify(irrigationWaterAnalysisRepository).save(irrigationCaptor.capture());
        IrrigationWaterAnalysis migratedIrrigation = irrigationCaptor.getValue();
        assertTrue(Boolean.TRUE.equals(migratedIrrigation.getLegacyDerived()));
        assertEquals(1001, migratedIrrigation.getLegacyEventId());
        assertEquals(new BigDecimal("5.4000"), migratedIrrigation.getLegacyNContributionKg());
        assertEquals(new BigDecimal("0.0000"), migratedIrrigation.getIrrigationVolumeM3());

        ArgumentCaptor<SoilTest> soilCaptor = ArgumentCaptor.forClass(SoilTest.class);
        verify(soilTestRepository).save(soilCaptor.capture());
        SoilTest migratedSoil = soilCaptor.getValue();
        assertTrue(Boolean.TRUE.equals(migratedSoil.getLegacyDerived()));
        assertEquals(1002, migratedSoil.getLegacyEventId());
        assertEquals(new BigDecimal("6.0000"), migratedSoil.getLegacyNContributionKg());
        assertEquals(new BigDecimal("3.0000"), migratedSoil.getMineralNKgPerHa());
    }

    @Test
    @DisplayName("Backfill is idempotent: already migrated events are skipped")
    void backfill_WhenAlreadyMigrated_ShouldSkipWithoutDuplicateWrites() {
        NutrientInputEvent irrigationEvent = NutrientInputEvent.builder()
                .id(2001)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("2.0000"))
                .build();
        NutrientInputEvent soilEvent = NutrientInputEvent.builder()
                .id(2002)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.SOIL_LEGACY)
                .nKg(new BigDecimal("4.0000"))
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(any()))
                .thenReturn(List.of(irrigationEvent, soilEvent));
        when(irrigationWaterAnalysisRepository.existsByLegacyEventId(2001)).thenReturn(true);
        when(soilTestRepository.existsByLegacyEventId(2002)).thenReturn(true);

        LegacyNutrientInputBackfillService.BackfillReport report = service.backfillLegacyAggregateInputs();

        assertEquals(2, report.getScannedCount());
        assertEquals(0, report.getMigratedIrrigationCount());
        assertEquals(0, report.getMigratedSoilCount());
        assertEquals(2, report.getSkippedAlreadyMigratedCount());
        verify(irrigationWaterAnalysisRepository, never()).save(any(IrrigationWaterAnalysis.class));
        verify(soilTestRepository, never()).save(any(SoilTest.class));
    }

    @Test
    @DisplayName("Backfill skips invalid context rows and keeps deterministic counters")
    void backfill_WhenEventContextInvalid_ShouldSkipAsInvalid() {
        NutrientInputEvent invalid = NutrientInputEvent.builder()
                .id(3001)
                .season(season)
                .plot(null)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("1.0000"))
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(eq(List.of(
                NutrientInputSource.IRRIGATION_WATER,
                NutrientInputSource.SOIL_LEGACY
        )))).thenReturn(List.of(invalid));

        LegacyNutrientInputBackfillService.BackfillReport report = service.backfillLegacyAggregateInputs();

        assertEquals(1, report.getScannedCount());
        assertEquals(0, report.getMigratedIrrigationCount());
        assertEquals(1, report.getSkippedInvalidContextCount());
        verify(irrigationWaterAnalysisRepository, never()).save(any(IrrigationWaterAnalysis.class));
    }

    @Test
    @DisplayName("Backfill dry-run reports eligible records without writing dedicated tables")
    void backfill_WhenDryRunEnabled_ShouldReportWouldMigrateWithoutSaving() {
        NutrientInputEvent irrigationEvent = NutrientInputEvent.builder()
                .id(4001)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("3.5000"))
                .appliedDate(LocalDate.of(2026, 3, 12))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();
        NutrientInputEvent soilEvent = NutrientInputEvent.builder()
                .id(4002)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.SOIL_LEGACY)
                .nKg(new BigDecimal("2.2000"))
                .appliedDate(LocalDate.of(2026, 3, 12))
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(any()))
                .thenReturn(List.of(irrigationEvent, soilEvent));
        when(irrigationWaterAnalysisRepository.existsByLegacyEventId(4001)).thenReturn(false);
        when(soilTestRepository.existsByLegacyEventId(4002)).thenReturn(false);

        LegacyNutrientInputBackfillService.BackfillReport report =
                service.backfillLegacyAggregateInputs(
                        LegacyNutrientInputBackfillService.BackfillOptions.builder()
                                .dryRun(true)
                                .sampleLimit(10)
                                .build()
                );

        assertTrue(report.isDryRun());
        assertEquals(1, report.getEligibleIrrigationCount());
        assertEquals(1, report.getEligibleSoilCount());
        assertEquals(2, report.getWouldMigrateCount());
        assertEquals(1, report.getWouldMigrateIrrigationCount());
        assertEquals(1, report.getWouldMigrateSoilCount());
        assertEquals(0, report.getMigratedIrrigationCount());
        assertEquals(0, report.getMigratedSoilCount());
        assertEquals(
                List.of("would_migrate", "would_migrate"),
                report.getSampleDecisions().stream().map(LegacyNutrientInputBackfillService.RecordDecision::getAction).collect(Collectors.toList())
        );
        verify(irrigationWaterAnalysisRepository, never()).save(any(IrrigationWaterAnalysis.class));
        verify(soilTestRepository, never()).save(any(SoilTest.class));
    }

    @Test
    @DisplayName("Backfill applies season/date scope and reports skip reasons")
    void backfill_WhenScopedBySeasonAndDate_ShouldRespectScopeAndReasons() {
        NutrientInputEvent inRangeIrrigation = NutrientInputEvent.builder()
                .id(5001)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("2.5000"))
                .appliedDate(LocalDate.of(2026, 3, 10))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();
        NutrientInputEvent outOfRangeSoil = NutrientInputEvent.builder()
                .id(5002)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.SOIL_LEGACY)
                .nKg(new BigDecimal("3.0000"))
                .appliedDate(LocalDate.of(2026, 3, 8))
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .build();
        NutrientInputEvent alreadyMigratedSoil = NutrientInputEvent.builder()
                .id(5003)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.SOIL_LEGACY)
                .nKg(new BigDecimal("4.0000"))
                .appliedDate(LocalDate.of(2026, 3, 11))
                .sourceType(NutrientInputSourceType.SYSTEM_ESTIMATED)
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInAndSeasonIdOrderByIdAsc(any(), eq(33)))
                .thenReturn(List.of(inRangeIrrigation, outOfRangeSoil, alreadyMigratedSoil));
        when(irrigationWaterAnalysisRepository.existsByLegacyEventId(5001)).thenReturn(false);
        when(soilTestRepository.existsByLegacyEventId(5003)).thenReturn(true);
        when(irrigationWaterAnalysisRepository.save(any(IrrigationWaterAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, IrrigationWaterAnalysis.class));

        LegacyNutrientInputBackfillService.BackfillReport report =
                service.backfillLegacyAggregateInputs(
                        LegacyNutrientInputBackfillService.BackfillOptions.builder()
                                .dryRun(false)
                                .seasonId(33)
                                .fromDate(LocalDate.of(2026, 3, 10))
                                .toDate(LocalDate.of(2026, 3, 11))
                                .sampleLimit(20)
                                .build()
                );

        assertEquals(3, report.getScannedCount());
        assertEquals(1, report.getMigratedIrrigationCount());
        assertEquals(0, report.getMigratedSoilCount());
        assertEquals(1, report.getSkippedOutOfScopeCount());
        assertEquals(1, report.getSkippedAlreadyMigratedCount());
        assertEquals(0, report.getSkippedOutOfScopeIrrigationCount());
        assertEquals(1, report.getSkippedOutOfScopeSoilCount());
        assertEquals(0, report.getSkippedAlreadyMigratedIrrigationCount());
        assertEquals(1, report.getSkippedAlreadyMigratedSoilCount());
        assertEquals(1, report.getEligibleIrrigationCount());
        assertEquals(0, report.getEligibleSoilCount());
    }

    @Test
    @DisplayName("Backfill reports conflict when dedicated write fails and record is not already migrated")
    void backfill_WhenDedicatedWriteConflicts_ShouldTrackConflictCounters() {
        NutrientInputEvent irrigationEvent = NutrientInputEvent.builder()
                .id(6001)
                .season(season)
                .plot(plot)
                .inputSource(NutrientInputSource.IRRIGATION_WATER)
                .nKg(new BigDecimal("3.3000"))
                .appliedDate(LocalDate.of(2026, 3, 15))
                .sourceType(NutrientInputSourceType.USER_ENTERED)
                .build();

        when(nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(any()))
                .thenReturn(List.of(irrigationEvent));
        when(irrigationWaterAnalysisRepository.existsByLegacyEventId(6001))
                .thenReturn(false, false);
        when(irrigationWaterAnalysisRepository.save(any(IrrigationWaterAnalysis.class)))
                .thenThrow(new DataIntegrityViolationException("simulated conflict"));

        LegacyNutrientInputBackfillService.BackfillReport report =
                service.backfillLegacyAggregateInputs(
                        LegacyNutrientInputBackfillService.BackfillOptions.builder()
                                .dryRun(false)
                                .sampleLimit(10)
                                .build()
                );

        assertEquals(1, report.getScannedCount());
        assertEquals(1, report.getEligibleCount());
        assertEquals(1, report.getEligibleIrrigationCount());
        assertEquals(0, report.getMigratedCount());
        assertEquals(1, report.getSkippedCount());
        assertEquals(1, report.getSkippedConflictCount());
        assertEquals(1, report.getSkippedConflictIrrigationCount());
        assertEquals(0, report.getSkippedConflictSoilCount());
        assertEquals(1, report.getInvalidOrConflictCount());
        assertEquals(
                List.of("conflict"),
                report.getSampleDecisions().stream()
                        .map(LegacyNutrientInputBackfillService.RecordDecision::getReason)
                        .toList()
        );
    }

    @Test
    @DisplayName("Backfill rejects invalid date range options")
    void backfill_WhenDateRangeInvalid_ShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.backfillLegacyAggregateInputs(
                        LegacyNutrientInputBackfillService.BackfillOptions.builder()
                                .fromDate(LocalDate.of(2026, 3, 20))
                                .toDate(LocalDate.of(2026, 3, 10))
                                .build()
                )
        );
    }
}
