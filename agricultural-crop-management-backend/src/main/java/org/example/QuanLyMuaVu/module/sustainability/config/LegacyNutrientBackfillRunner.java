package org.example.QuanLyMuaVu.module.sustainability.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.module.sustainability.service.LegacyNutrientInputBackfillService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LegacyNutrientBackfillRunner implements ApplicationRunner {

    AppProperties appProperties;
    LegacyNutrientInputBackfillService legacyNutrientInputBackfillService;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Sustainability sustainability = appProperties.getSustainability();
        if (sustainability == null
                || sustainability.getLegacyBackfill() == null
                || !sustainability.getLegacyBackfill().isEnabled()) {
            return;
        }

        AppProperties.LegacyBackfill cfg = sustainability.getLegacyBackfill();
        LegacyNutrientInputBackfillService.BackfillOptions options =
                LegacyNutrientInputBackfillService.BackfillOptions.builder()
                        .dryRun(cfg.isDryRun())
                        .seasonId(cfg.getSeasonId())
                        .fromDate(cfg.getFromDate())
                        .toDate(cfg.getToDate())
                        .sampleLimit(cfg.getSampleLimit() != null ? cfg.getSampleLimit() : 50)
                        .build();

        LegacyNutrientInputBackfillService.BackfillReport report =
                legacyNutrientInputBackfillService.backfillLegacyAggregateInputs(options);

        log.info(
                "Legacy nutrient-input backfill completed: dryRun={}, seasonFilter={}, fromDate={}, toDate={}, scannedTotal={}, scannedIrrigation={}, scannedSoil={}, eligibleTotal={}, eligibleIrrigation={}, eligibleSoil={}, wouldMigrateTotal={}, wouldMigrateIrrigation={}, wouldMigrateSoil={}, migratedTotal={}, migratedIrrigation={}, migratedSoil={}, skippedTotal={}, skippedAlreadyMigrated={}, skippedAlreadyMigratedIrrigation={}, skippedAlreadyMigratedSoil={}, skippedInvalid={}, skippedInvalidIrrigation={}, skippedInvalidSoil={}, skippedConflict={}, skippedConflictIrrigation={}, skippedConflictSoil={}, invalidOrConflictTotal={}, skippedOutOfScope={}, skippedOutOfScopeIrrigation={}, skippedOutOfScopeSoil={}, skippedUnsupported={}",
                report.isDryRun(),
                report.getSeasonFilterSeasonId(),
                report.getFromDateFilter(),
                report.getToDateFilter(),
                report.getScannedCount(),
                report.getScannedIrrigationCount(),
                report.getScannedSoilCount(),
                report.getEligibleCount(),
                report.getEligibleIrrigationCount(),
                report.getEligibleSoilCount(),
                report.getWouldMigrateCount(),
                report.getWouldMigrateIrrigationCount(),
                report.getWouldMigrateSoilCount(),
                report.getMigratedCount(),
                report.getMigratedIrrigationCount(),
                report.getMigratedSoilCount(),
                report.getSkippedCount(),
                report.getSkippedAlreadyMigratedCount(),
                report.getSkippedAlreadyMigratedIrrigationCount(),
                report.getSkippedAlreadyMigratedSoilCount(),
                report.getSkippedInvalidContextCount(),
                report.getSkippedInvalidIrrigationCount(),
                report.getSkippedInvalidSoilCount(),
                report.getSkippedConflictCount(),
                report.getSkippedConflictIrrigationCount(),
                report.getSkippedConflictSoilCount(),
                report.getInvalidOrConflictCount(),
                report.getSkippedOutOfScopeCount(),
                report.getSkippedOutOfScopeIrrigationCount(),
                report.getSkippedOutOfScopeSoilCount(),
                report.getSkippedUnsupportedSourceCount()
        );

        if (report.getSampleDecisions() != null && !report.getSampleDecisions().isEmpty()) {
            report.getSampleDecisions().forEach(item -> log.info(
                    "Legacy backfill decision: legacyEventId={}, source={}, action={}, reason={}, seasonId={}, plotId={}, recordDate={}",
                    item.getLegacyEventId(),
                    item.getSource(),
                    item.getAction(),
                    item.getReason(),
                    item.getSeasonId(),
                    item.getPlotId(),
                    item.getRecordDate()
            ));
        }
    }
}
