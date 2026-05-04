package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;

/**
 * Service responsible for Season business rule validation.
 * Single Responsibility: Validation logic only.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonValidationService {

    SeasonRepository seasonRepository;

    /**
     * BR8/BR12: Validate that season name is unique within the plot.
     *
     * @param plotId     the plot ID
     * @param seasonName the season name to validate
     * @param excludeId  the season ID to exclude (null for create, season ID for
     *                   update)
     * @throws AppException if season name already exists in the plot
     */
    public void validateSeasonNameUniquenessInPlot(Integer plotId, String seasonName, Integer excludeId) {
        boolean exists = seasonRepository.existsByPlotIdAndSeasonNameIgnoreCaseExcluding(
                plotId, seasonName, excludeId);
        if (exists) {
            throw new AppException(ErrorCode.SEASON_NAME_EXISTS_IN_PLOT);
        }
    }

    /**
     * Validate that no overlapping active or planned seasons exist on the same
     * plot.
     *
     * @param plot               the plot to check
     * @param start              the start date of the new/updated season
     * @param plannedHarvestDate the planned harvest date
     * @param end                the end date (optional)
     * @param excludeSeasonId    season ID to exclude from check (for updates)
     * @throws AppException if overlapping season found
     */
    public void validateNoOverlappingActiveOrPlannedSeasons(
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            LocalDate start,
            LocalDate plannedHarvestDate,
            LocalDate end,
            Integer excludeSeasonId) {

        if (plot == null || start == null) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        LocalDate newStart = start;
        LocalDate newEnd = end != null ? end : plannedHarvestDate;

        List<Season> existing = seasonRepository.findAllByPlot_Id(plot.getId());
        for (Season other : existing) {
            if (excludeSeasonId != null && excludeSeasonId.equals(other.getId())) {
                continue;
            }
            SeasonStatus status = other.getStatus();
            if (status == null
                    || (status != SeasonStatus.PLANNED && status != SeasonStatus.ACTIVE)) {
                continue;
            }

            LocalDate otherStart = other.getStartDate();
            LocalDate otherEnd = other.getEndDate() != null
                    ? other.getEndDate()
                    : other.getPlannedHarvestDate();

            if (otherStart == null && otherEnd == null) {
                // Conservatively treat undefined ranges as overlapping
                throw new AppException(ErrorCode.SEASON_OVERLAP);
            }

            if (rangesOverlap(newStart, newEnd, otherStart, otherEnd)) {
                throw new AppException(ErrorCode.SEASON_OVERLAP);
            }
        }
    }

    /**
     * Check if two date ranges overlap.
     *
     * @param start1 start of first range
     * @param end1   end of first range (can be null for open-ended)
     * @param start2 start of second range
     * @param end2   end of second range (can be null for open-ended)
     * @return true if ranges overlap
     */
    public boolean rangesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || start2 == null) {
            return true;
        }

        if (end1 != null && end2 != null) {
            return !end1.isBefore(start2) && !end2.isBefore(start1);
        }
        if (end1 == null && end2 != null) {
            return !end2.isBefore(start1);
        }
        if (end1 != null) { // end2 == null
            return !end1.isBefore(start2);
        }
        // both open-ended
        return true;
    }

    /**
     * Validate season date constraints.
     *
     * @param startDate          the start date
     * @param endDate            the end date (optional)
     * @param plannedHarvestDate the planned harvest date (optional)
     * @throws AppException if dates are invalid
     */
    public void validateSeasonDates(LocalDate startDate, LocalDate endDate, LocalDate plannedHarvestDate) {
        if (startDate == null) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
        if (plannedHarvestDate != null && plannedHarvestDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    /**
     * BR102/BR106: ValidateDataFormat() - Validates input fields for Create/Update
     * Season.
     * Checks for empty mandatory fields, special characters in season name, and
     * date format/logic.
     *
     * @param seasonName  the season name (mandatory, no special characters)
     * @param startDate   the start date (mandatory)
     * @param endDate     the end date (mandatory per BR102/BR106)
     * @param plotId      the plot ID (mandatory)
     * @param description optional description field
     * @throws AppException MSG_1 if mandatory field is empty
     * @throws AppException MSG_4 if format is invalid (special chars) or dates are
     *                      illogical
     */
    public void ValidateDataFormat(String seasonName, LocalDate startDate, LocalDate endDate,
            Integer plotId, String description) {
        // MSG 1: Check mandatory fields
        if (seasonName == null || seasonName.trim().isEmpty()) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (startDate == null) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (endDate == null) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (plotId == null) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }

        // MSG 4: Check seasonName for special characters
        // Allowed: Unicode letters (including Vietnamese), digits, spaces, hyphens,
        // underscores
        if (!seasonName.matches("^[\\p{L}\\p{M}0-9\\s\\-_]+$")) {
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }

        // MSG 4: Check date logic - startDate must be before or equal to endDate
        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
    }
}
