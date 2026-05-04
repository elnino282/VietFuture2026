package org.example.QuanLyMuaVu.module.shared.pattern.Chain.Validators;

import java.time.LocalDate;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.ValidationHandler;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: Date Range Validator.
 * <p>
 * Validates temporal constraints:
 * 1. Start date is required and not null
 * 2. If planned harvest date is provided, it must be >= start date
 * 3. If end date is provided, it must be >= start date
 * 4. If both end and planned harvest dates exist, end >= planned harvest
 */
@Component
public class DateRangeValidator extends ValidationHandler<SeasonValidationContext> {

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        LocalDate startDate = ctx.getStartDate();
        LocalDate plannedHarvestDate = ctx.getPlannedHarvestDate();
        LocalDate endDate = ctx.getEndDate();

        // Start date is mandatory
        if (startDate == null) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        // Planned harvest date must be after or equal to start date
        if (plannedHarvestDate != null && plannedHarvestDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        // End date must be after or equal to start date
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        // If both dates exist, end date should be >= planned harvest date
        if (endDate != null && plannedHarvestDate != null && endDate.isBefore(plannedHarvestDate)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    @Override
    public String getValidatorName() {
        return "DateRangeValidator";
    }
}
