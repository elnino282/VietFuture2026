package org.example.QuanLyMuaVu.Config;

import org.example.QuanLyMuaVu.module.sustainability.config.LegacyNutrientBackfillRunner;
import org.example.QuanLyMuaVu.module.sustainability.service.LegacyNutrientInputBackfillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyNutrientBackfillRunnerTest {

    @Mock
    private LegacyNutrientInputBackfillService backfillService;

    @Test
    @DisplayName("Runner does not execute when legacy backfill is disabled")
    void run_WhenDisabled_ShouldSkipExecution() {
        AppProperties appProperties = new AppProperties();
        appProperties.getSustainability().getLegacyBackfill().setEnabled(false);

        LegacyNutrientBackfillRunner runner = new LegacyNutrientBackfillRunner(appProperties, backfillService);
        runner.run(new DefaultApplicationArguments(new String[]{}));

        verify(backfillService, never()).backfillLegacyAggregateInputs(
                org.mockito.ArgumentMatchers.any(LegacyNutrientInputBackfillService.BackfillOptions.class)
        );
    }

    @Test
    @DisplayName("Runner forwards configured options to backfill service")
    void run_WhenEnabled_ShouldUseConfiguredOptions() {
        AppProperties appProperties = new AppProperties();
        AppProperties.LegacyBackfill cfg = appProperties.getSustainability().getLegacyBackfill();
        cfg.setEnabled(true);
        cfg.setDryRun(true);
        cfg.setSeasonId(33);
        cfg.setFromDate(LocalDate.of(2026, 3, 1));
        cfg.setToDate(LocalDate.of(2026, 3, 31));
        cfg.setSampleLimit(12);

        when(backfillService.backfillLegacyAggregateInputs(
                org.mockito.ArgumentMatchers.any(LegacyNutrientInputBackfillService.BackfillOptions.class)))
                .thenReturn(LegacyNutrientInputBackfillService.BackfillReport.builder()
                        .dryRun(true)
                        .seasonFilterSeasonId(33)
                        .scannedCount(0)
                        .sampleDecisions(List.of())
                        .build());

        LegacyNutrientBackfillRunner runner = new LegacyNutrientBackfillRunner(appProperties, backfillService);
        runner.run(new DefaultApplicationArguments(new String[]{}));

        ArgumentCaptor<LegacyNutrientInputBackfillService.BackfillOptions> optionsCaptor =
                ArgumentCaptor.forClass(LegacyNutrientInputBackfillService.BackfillOptions.class);
        verify(backfillService).backfillLegacyAggregateInputs(optionsCaptor.capture());

        LegacyNutrientInputBackfillService.BackfillOptions options = optionsCaptor.getValue();
        assertTrue(options.isDryRun());
        assertEquals(33, options.getSeasonId());
        assertEquals(LocalDate.of(2026, 3, 1), options.getFromDate());
        assertEquals(LocalDate.of(2026, 3, 31), options.getToDate());
        assertEquals(12, options.getSampleLimit());
    }
}
