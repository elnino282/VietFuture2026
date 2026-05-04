package org.example.QuanLyMuaVu.module.shared.pattern.Chain.Validators;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.ValidationHandler;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: Active org.example.QuanLyMuaVu.module.season.entity.Season Overlap Validator.
 * <p>
 * Enforces the critical business rule:
 * "Cannot start a new season on a plot with an active ongoing season"
 * <p>
 * Checks for overlapping date ranges with existing PLANNED or ACTIVE seasons
 * on the same plot.
 */
@Component
@RequiredArgsConstructor
public class ActiveSeasonValidator extends ValidationHandler<SeasonValidationContext> {

    private final SeasonQueryPort seasonQueryPort;

    private static final Set<SeasonStatus> BLOCKING_STATUSES = EnumSet.of(
            SeasonStatus.PLANNED,
            SeasonStatus.ACTIVE);

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ctx.getPlot();
        if (plot == null) {
            // org.example.QuanLyMuaVu.module.farm.entity.Plot should have been populated by PlotOwnershipValidator
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        LocalDate newStart = ctx.getStartDate();
        LocalDate newEnd = ctx.getPlannedHarvestDate() != null
                ? ctx.getPlannedHarvestDate()
                : ctx.getEndDate();

        // If no end date, use a far-future date for overlap calculation
        if (newEnd == null) {
            newEnd = LocalDate.of(9999, 12, 31);
        }

        // Find all non-terminal seasons on this plot
        List<org.example.QuanLyMuaVu.module.season.entity.Season> existingSeasons = seasonQueryPort.findAllSeasonsByPlotId(plot.getId()).stream()
                .filter(season -> season.getStatus() != null && BLOCKING_STATUSES.contains(season.getStatus()))
                .toList();

        for (org.example.QuanLyMuaVu.module.season.entity.Season existing : existingSeasons) {
            if (rangesOverlap(newStart, newEnd,
                    existing.getStartDate(),
                    getEffectiveEndDate(existing))) {
                throw new AppException(ErrorCode.SEASON_OVERLAP);
            }
        }
    }

    /**
     * Gets the effective end date for overlap calculation.
     * Priority: endDate > plannedHarvestDate > far future
     */
    private LocalDate getEffectiveEndDate(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season.getEndDate() != null) {
            return season.getEndDate();
        }
        if (season.getPlannedHarvestDate() != null) {
            return season.getPlannedHarvestDate();
        }
        return LocalDate.of(9999, 12, 31);
    }

    /**
     * Checks if two date ranges overlap.
     */
    private boolean rangesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        // Ranges overlap if: start1 <= end2 AND start2 <= end1
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    @Override
    public String getValidatorName() {
        return "ActiveSeasonValidator";
    }
}
