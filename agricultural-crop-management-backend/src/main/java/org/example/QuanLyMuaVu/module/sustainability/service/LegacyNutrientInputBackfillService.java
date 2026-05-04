package org.example.QuanLyMuaVu.module.sustainability.service;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.module.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class LegacyNutrientInputBackfillService {

    static final List<NutrientInputSource> LEGACY_SOURCES = List.of(
            NutrientInputSource.IRRIGATION_WATER,
            NutrientInputSource.SOIL_LEGACY
    );

    static final BigDecimal ZERO = BigDecimal.ZERO;

    static final String ACTION_MIGRATED = "migrated";
    static final String ACTION_WOULD_MIGRATE = "would_migrate";
    static final String ACTION_SKIPPED = "skipped";

    static final String REASON_ALREADY_MIGRATED = "already_migrated";
    static final String REASON_INVALID_CONTEXT = "invalid_context";
    static final String REASON_OUT_OF_SCOPE = "out_of_scope";
    static final String REASON_UNSUPPORTED_SOURCE = "unsupported_source";
    static final String REASON_CONFLICT = "conflict";

    NutrientInputEventRepository nutrientInputEventRepository;
    IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    SoilTestRepository soilTestRepository;

    public BackfillReport backfillLegacyAggregateInputs() {
        return backfillLegacyAggregateInputs(
                BackfillOptions.builder()
                        .dryRun(false)
                        .build()
        );
    }

    public BackfillReport backfillLegacyAggregateInputs(BackfillOptions options) {
        BackfillOptions effectiveOptions = normalizeOptions(options);
        validateOptions(effectiveOptions);

        List<NutrientInputEvent> legacyEvents = loadLegacyEvents(effectiveOptions.getSeasonId());
        List<RecordDecision> decisions = new ArrayList<>();

        int scannedIrrigation = 0;
        int scannedSoil = 0;
        int eligibleIrrigation = 0;
        int eligibleSoil = 0;
        int wouldMigrateIrrigation = 0;
        int wouldMigrateSoil = 0;
        int migratedIrrigation = 0;
        int migratedSoil = 0;
        int skippedAlreadyMigrated = 0;
        int skippedInvalidContext = 0;
        int skippedOutOfScope = 0;
        int skippedUnsupported = 0;
        int skippedConflict = 0;
        int skippedAlreadyMigratedIrrigation = 0;
        int skippedAlreadyMigratedSoil = 0;
        int skippedInvalidIrrigation = 0;
        int skippedInvalidSoil = 0;
        int skippedOutOfScopeIrrigation = 0;
        int skippedOutOfScopeSoil = 0;
        int skippedConflictIrrigation = 0;
        int skippedConflictSoil = 0;

        for (NutrientInputEvent event : legacyEvents) {
            NutrientInputSource source = event != null ? event.getInputSource() : null;
            if (source == NutrientInputSource.IRRIGATION_WATER) {
                scannedIrrigation++;
            } else if (source == NutrientInputSource.SOIL_LEGACY) {
                scannedSoil++;
            } else {
                skippedUnsupported++;
                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_UNSUPPORTED_SOURCE);
                continue;
            }

            if (!isInDateScope(event, effectiveOptions)) {
                skippedOutOfScope++;
                if (source == NutrientInputSource.IRRIGATION_WATER) {
                    skippedOutOfScopeIrrigation++;
                } else if (source == NutrientInputSource.SOIL_LEGACY) {
                    skippedOutOfScopeSoil++;
                }
                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_OUT_OF_SCOPE);
                continue;
            }

            if (!hasValidContext(event)) {
                skippedInvalidContext++;
                if (source == NutrientInputSource.IRRIGATION_WATER) {
                    skippedInvalidIrrigation++;
                } else if (source == NutrientInputSource.SOIL_LEGACY) {
                    skippedInvalidSoil++;
                }
                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_INVALID_CONTEXT);
                continue;
            }

            Integer legacyEventId = event.getId();
            if (legacyEventId == null) {
                skippedInvalidContext++;
                if (source == NutrientInputSource.IRRIGATION_WATER) {
                    skippedInvalidIrrigation++;
                } else if (source == NutrientInputSource.SOIL_LEGACY) {
                    skippedInvalidSoil++;
                }
                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_INVALID_CONTEXT);
                continue;
            }

            switch (source) {
                case IRRIGATION_WATER -> {
                    if (irrigationWaterAnalysisRepository.existsByLegacyEventId(legacyEventId)) {
                        skippedAlreadyMigrated++;
                        skippedAlreadyMigratedIrrigation++;
                        appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_ALREADY_MIGRATED);
                        continue;
                    }
                    eligibleIrrigation++;
                    if (!effectiveOptions.isDryRun()) {
                        try {
                            irrigationWaterAnalysisRepository.save(mapLegacyIrrigationEvent(event));
                            migratedIrrigation++;
                            appendDecision(decisions, effectiveOptions, event, source, ACTION_MIGRATED, null);
                        } catch (DataIntegrityViolationException ex) {
                            if (irrigationWaterAnalysisRepository.existsByLegacyEventId(legacyEventId)) {
                                skippedAlreadyMigrated++;
                                skippedAlreadyMigratedIrrigation++;
                                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_ALREADY_MIGRATED);
                            } else {
                                skippedConflict++;
                                skippedConflictIrrigation++;
                                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_CONFLICT);
                            }
                        }
                    } else {
                        wouldMigrateIrrigation++;
                        appendDecision(decisions, effectiveOptions, event, source, ACTION_WOULD_MIGRATE, null);
                    }
                }
                case SOIL_LEGACY -> {
                    if (soilTestRepository.existsByLegacyEventId(legacyEventId)) {
                        skippedAlreadyMigrated++;
                        skippedAlreadyMigratedSoil++;
                        appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_ALREADY_MIGRATED);
                        continue;
                    }
                    eligibleSoil++;
                    if (!effectiveOptions.isDryRun()) {
                        try {
                            soilTestRepository.save(mapLegacySoilEvent(event));
                            migratedSoil++;
                            appendDecision(decisions, effectiveOptions, event, source, ACTION_MIGRATED, null);
                        } catch (DataIntegrityViolationException ex) {
                            if (soilTestRepository.existsByLegacyEventId(legacyEventId)) {
                                skippedAlreadyMigrated++;
                                skippedAlreadyMigratedSoil++;
                                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_ALREADY_MIGRATED);
                            } else {
                                skippedConflict++;
                                skippedConflictSoil++;
                                appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_CONFLICT);
                            }
                        }
                    } else {
                        wouldMigrateSoil++;
                        appendDecision(decisions, effectiveOptions, event, source, ACTION_WOULD_MIGRATE, null);
                    }
                }
                default -> {
                    skippedUnsupported++;
                    appendDecision(decisions, effectiveOptions, event, source, ACTION_SKIPPED, REASON_UNSUPPORTED_SOURCE);
                }
            }
        }

        int eligibleCount = eligibleIrrigation + eligibleSoil;
        int wouldMigrateCount = wouldMigrateIrrigation + wouldMigrateSoil;
        int migratedCount = migratedIrrigation + migratedSoil;
        int skippedCount = skippedAlreadyMigrated
                + skippedInvalidContext
                + skippedOutOfScope
                + skippedUnsupported
                + skippedConflict;
        int invalidOrConflictCount = skippedInvalidContext + skippedConflict;

        return BackfillReport.builder()
                .dryRun(effectiveOptions.isDryRun())
                .seasonFilterSeasonId(effectiveOptions.getSeasonId())
                .fromDateFilter(effectiveOptions.getFromDate())
                .toDateFilter(effectiveOptions.getToDate())
                .scannedCount(legacyEvents.size())
                .scannedIrrigationCount(scannedIrrigation)
                .scannedSoilCount(scannedSoil)
                .eligibleCount(eligibleCount)
                .eligibleIrrigationCount(eligibleIrrigation)
                .eligibleSoilCount(eligibleSoil)
                .wouldMigrateCount(wouldMigrateCount)
                .wouldMigrateIrrigationCount(wouldMigrateIrrigation)
                .wouldMigrateSoilCount(wouldMigrateSoil)
                .migratedCount(migratedCount)
                .migratedIrrigationCount(migratedIrrigation)
                .migratedSoilCount(migratedSoil)
                .skippedCount(skippedCount)
                .skippedAlreadyMigratedCount(skippedAlreadyMigrated)
                .skippedInvalidContextCount(skippedInvalidContext)
                .skippedOutOfScopeCount(skippedOutOfScope)
                .skippedUnsupportedSourceCount(skippedUnsupported)
                .skippedConflictCount(skippedConflict)
                .invalidOrConflictCount(invalidOrConflictCount)
                .skippedAlreadyMigratedIrrigationCount(skippedAlreadyMigratedIrrigation)
                .skippedAlreadyMigratedSoilCount(skippedAlreadyMigratedSoil)
                .skippedInvalidIrrigationCount(skippedInvalidIrrigation)
                .skippedInvalidSoilCount(skippedInvalidSoil)
                .skippedOutOfScopeIrrigationCount(skippedOutOfScopeIrrigation)
                .skippedOutOfScopeSoilCount(skippedOutOfScopeSoil)
                .skippedConflictIrrigationCount(skippedConflictIrrigation)
                .skippedConflictSoilCount(skippedConflictSoil)
                .sampleDecisions(List.copyOf(decisions))
                .build();
    }

    private BackfillOptions normalizeOptions(BackfillOptions options) {
        if (options == null) {
            return BackfillOptions.builder().build();
        }
        int sampleLimit = options.getSampleLimit() < 0 ? 0 : options.getSampleLimit();
        return BackfillOptions.builder()
                .dryRun(options.isDryRun())
                .seasonId(options.getSeasonId())
                .fromDate(options.getFromDate())
                .toDate(options.getToDate())
                .sampleLimit(sampleLimit)
                .build();
    }

    private void validateOptions(BackfillOptions options) {
        if (options.getFromDate() != null
                && options.getToDate() != null
                && options.getFromDate().isAfter(options.getToDate())) {
            throw new IllegalArgumentException("legacy backfill fromDate must be <= toDate");
        }
    }

    private List<NutrientInputEvent> loadLegacyEvents(Integer seasonId) {
        if (seasonId != null && seasonId > 0) {
            return nutrientInputEventRepository.findAllByInputSourceInAndSeasonIdOrderByIdAsc(LEGACY_SOURCES, seasonId);
        }
        return nutrientInputEventRepository.findAllByInputSourceInOrderByIdAsc(LEGACY_SOURCES);
    }

    private boolean isInDateScope(NutrientInputEvent event, BackfillOptions options) {
        if (event == null) {
            return false;
        }
        if (options.getFromDate() == null && options.getToDate() == null) {
            return true;
        }
        LocalDate recordDate = resolveRecordDate(event);
        if (recordDate == null) {
            return false;
        }
        if (options.getFromDate() != null && recordDate.isBefore(options.getFromDate())) {
            return false;
        }
        if (options.getToDate() != null && recordDate.isAfter(options.getToDate())) {
            return false;
        }
        return true;
    }

    private LocalDate resolveRecordDate(NutrientInputEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getAppliedDate() != null) {
            return event.getAppliedDate();
        }
        LocalDateTime createdAt = event.getCreatedAt();
        if (createdAt != null) {
            return createdAt.toLocalDate();
        }
        return null;
    }

    private void appendDecision(
            List<RecordDecision> decisions,
            BackfillOptions options,
            NutrientInputEvent event,
            NutrientInputSource source,
            String action,
            String reason
    ) {
        if (decisions == null || decisions.size() >= options.getSampleLimit()) {
            return;
        }
        decisions.add(RecordDecision.builder()
                .legacyEventId(event != null ? event.getId() : null)
                .source(source)
                .action(action)
                .reason(reason)
                .seasonId(event != null
                        ? event.getSeasonId() != null ? event.getSeasonId()
                                : event.getSeason() != null ? event.getSeason().getId() : null
                        : null)
                .plotId(event != null
                        ? event.getPlotId() != null ? event.getPlotId()
                                : event.getPlot() != null ? event.getPlot().getId() : null
                        : null)
                .recordDate(resolveRecordDate(event))
                .build());
    }

    private IrrigationWaterAnalysis mapLegacyIrrigationEvent(NutrientInputEvent event) {
        NutrientInputSourceType sourceType = resolveSourceType(event);
        return IrrigationWaterAnalysis.builder()
                .seasonId(resolveSeasonId(event))
                .season(event.getSeason())
                .plotId(resolvePlotId(event))
                .plot(event.getPlot())
                .sampleDate(resolveSampleDate(event))
                .nitrateMgPerL(null)
                .ammoniumMgPerL(null)
                .totalNmgPerL(null)
                .irrigationVolumeM3(scale4(ZERO))
                .legacyNContributionKg(scale4(safe(event.getNKg())))
                .legacyEventId(event.getId())
                .legacyDerived(Boolean.TRUE)
                .measured(resolveMeasured(event, sourceType))
                .sourceType(sourceType)
                .sourceDocument(trimToNull(event.getSourceDocument()))
                .labReference("LEGACY_NUTRIENT_EVENT#" + event.getId())
                .note(appendMigrationNote(
                        event.getNote(),
                        "Migrated from aggregate IRRIGATION_WATER event (legacy_contribution_only)."
                ))
                .createdByUserId(event.getCreatedByUserId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private SoilTest mapLegacySoilEvent(NutrientInputEvent event) {
        NutrientInputSourceType sourceType = resolveSourceType(event);
        BigDecimal area = event.getPlot() != null ? event.getPlot().getArea() : null;
        BigDecimal mineralPerHa;
        String noteSuffix;
        if (area != null && area.compareTo(ZERO) > 0) {
            mineralPerHa = safe(event.getNKg()).divide(area, 8, RoundingMode.HALF_UP);
            noteSuffix = "Migrated from aggregate SOIL_LEGACY event.";
        } else {
            mineralPerHa = ZERO;
            noteSuffix = "Migrated from aggregate SOIL_LEGACY event; plot area missing so mineral_n_kg_per_ha set to 0 and legacy contribution preserved.";
        }

        return SoilTest.builder()
                .seasonId(resolveSeasonId(event))
                .season(event.getSeason())
                .plotId(resolvePlotId(event))
                .plot(event.getPlot())
                .sampleDate(resolveSampleDate(event))
                .soilOrganicMatterPct(null)
                .mineralNKgPerHa(scale4(mineralPerHa))
                .nitrateMgPerKg(null)
                .ammoniumMgPerKg(null)
                .legacyNContributionKg(scale4(safe(event.getNKg())))
                .legacyEventId(event.getId())
                .legacyDerived(Boolean.TRUE)
                .measured(resolveMeasured(event, sourceType))
                .sourceType(sourceType)
                .sourceDocument(trimToNull(event.getSourceDocument()))
                .labReference("LEGACY_NUTRIENT_EVENT#" + event.getId())
                .note(appendMigrationNote(event.getNote(), noteSuffix))
                .createdByUserId(event.getCreatedByUserId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private boolean hasValidContext(NutrientInputEvent event) {
        return event != null
                && resolveSeasonId(event) != null
                && resolvePlotId(event) != null;
    }

    private Integer resolveSeasonId(NutrientInputEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getSeasonId() != null) {
            return event.getSeasonId();
        }
        return event.getSeason() != null ? event.getSeason().getId() : null;
    }

    private Integer resolvePlotId(NutrientInputEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getPlotId() != null) {
            return event.getPlotId();
        }
        return event.getPlot() != null ? event.getPlot().getId() : null;
    }

    private LocalDate resolveSampleDate(NutrientInputEvent event) {
        LocalDate recordDate = resolveRecordDate(event);
        return recordDate != null ? recordDate : LocalDate.now();
    }

    private NutrientInputSourceType resolveSourceType(NutrientInputEvent event) {
        if (event.getSourceType() != null) {
            return event.getSourceType();
        }
        if (StringUtils.hasText(event.getDataSource())) {
            try {
                return NutrientInputSourceType.fromApiValue(event.getDataSource());
            } catch (IllegalArgumentException ignored) {
                // Fall through to deterministic fallback.
            }
        }
        if (Boolean.TRUE.equals(event.getMeasured())) {
            return NutrientInputSourceType.USER_ENTERED;
        }
        return NutrientInputSourceType.SYSTEM_ESTIMATED;
    }

    private boolean resolveMeasured(NutrientInputEvent event, NutrientInputSourceType sourceType) {
        return Boolean.TRUE.equals(event.getMeasured()) || (sourceType != null && sourceType.isMeasured());
    }

    private String appendMigrationNote(String original, String suffix) {
        if (!StringUtils.hasText(original)) {
            return suffix;
        }
        String normalized = original.trim();
        String suffixNormalized = suffix.trim();
        if (normalized.toLowerCase(Locale.ROOT).contains("migrated from aggregate")) {
            return normalized;
        }
        return normalized + " | " + suffixNormalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BackfillOptions {
        @Builder.Default
        boolean dryRun = true;
        Integer seasonId;
        LocalDate fromDate;
        LocalDate toDate;
        @Builder.Default
        int sampleLimit = 50;
    }

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecordDecision {
        Integer legacyEventId;
        NutrientInputSource source;
        String action;
        String reason;
        Integer seasonId;
        Integer plotId;
        LocalDate recordDate;
    }

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BackfillReport {
        boolean dryRun;
        Integer seasonFilterSeasonId;
        LocalDate fromDateFilter;
        LocalDate toDateFilter;
        int scannedCount;
        int scannedIrrigationCount;
        int scannedSoilCount;
        int eligibleCount;
        int eligibleIrrigationCount;
        int eligibleSoilCount;
        int wouldMigrateCount;
        int wouldMigrateIrrigationCount;
        int wouldMigrateSoilCount;
        int migratedCount;
        int migratedIrrigationCount;
        int migratedSoilCount;
        int skippedCount;
        int skippedAlreadyMigratedCount;
        int skippedInvalidContextCount;
        int skippedOutOfScopeCount;
        int skippedUnsupportedSourceCount;
        int skippedConflictCount;
        int invalidOrConflictCount;
        int skippedAlreadyMigratedIrrigationCount;
        int skippedAlreadyMigratedSoilCount;
        int skippedInvalidIrrigationCount;
        int skippedInvalidSoilCount;
        int skippedOutOfScopeIrrigationCount;
        int skippedOutOfScopeSoilCount;
        int skippedConflictIrrigationCount;
        int skippedConflictSoilCount;
        List<RecordDecision> sampleDecisions;
    }
}
