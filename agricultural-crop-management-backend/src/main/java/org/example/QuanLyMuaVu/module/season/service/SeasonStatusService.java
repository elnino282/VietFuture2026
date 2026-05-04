package org.example.QuanLyMuaVu.module.season.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.pattern.Strategy.SeasonStatusStrategy;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.season.dto.request.CancelSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CompleteSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.StartSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonStatusRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.SeasonMapper;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for Season status transitions.
 * Single Responsibility: Status management and lifecycle operations.
 * Uses Strategy Pattern for status transition validation.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonStatusService {

    SeasonRepository seasonRepository;
    HarvestRepository harvestRepository;
    TaskRepository taskRepository;
    SeasonMapper seasonMapper;
    FarmAccessPort farmAccessService;
    SeasonStatusStrategy statusStrategy;

    /**
     * Update season status with generic transition logic.
     */
    public SeasonResponse updateSeasonStatus(Integer id, UpdateSeasonStatusRequest request) {
        Season season = findAndValidateAccess(id);

        SeasonStatus targetStatus;
        try {
            targetStatus = SeasonStatus.fromCode(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        SeasonStatus currentStatus = season.getStatus();
        if (!statusStrategy.canTransition(currentStatus, targetStatus)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        if (targetStatus == SeasonStatus.ACTIVE && request.getActualStartDate() != null) {
            season.setStartDate(request.getActualStartDate());
        }
        if ((targetStatus == SeasonStatus.COMPLETED || targetStatus == SeasonStatus.CANCELLED
                || targetStatus == SeasonStatus.ARCHIVED)
                && request.getActualEndDate() != null) {
            LocalDate end = request.getActualEndDate();
            if (end.isBefore(season.getStartDate())) {
                throw new AppException(ErrorCode.INVALID_SEASON_DATES);
            }
            season.setEndDate(end);
        }

        season.setStatus(targetStatus);

        // When closing a season, sync actual yield from its harvest batches
        if (targetStatus == SeasonStatus.COMPLETED || targetStatus == SeasonStatus.ARCHIVED) {
            syncActualYieldFromHarvests(season);
        }

        Season saved = seasonRepository.save(season);
        return seasonMapper.toResponse(saved);
    }

    /**
     * BR23: Start a season: PLANNED → ACTIVE.
     * Validates that farm and plot are active before starting.
     */
    public SeasonResponse startSeason(Integer id, StartSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.ACTIVE)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        // Validate farm and plot are active
        if (season.getPlot() == null || season.getPlot().getFarm() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (season.getPlot().getFarm().getActive() == null || !season.getPlot().getFarm().getActive()) {
            throw new AppException(ErrorCode.FARM_INACTIVE);
        }

        // If actualStartDate provided, update startDate
        if (request != null && request.getActualStartDate() != null) {
            season.setStartDate(request.getActualStartDate());
        }

        season.setStatus(SeasonStatus.ACTIVE);
        Season saved = seasonRepository.save(season);
        return seasonMapper.toResponse(saved);
    }

    /**
     * BR27: Complete a season: ACTIVE → COMPLETED.
     * Checks for pending tasks and warns if forceComplete is not set.
     * Auto-calculates actual yield from harvests if not provided.
     */
    public SeasonResponse completeSeason(Integer id, CompleteSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.COMPLETED)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        // Validate end date
        LocalDate endDate = request.getEndDate();
        if (endDate.isBefore(season.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        // Check for pending/in-progress tasks
        long pendingOrInProgressTasks = taskRepository.countBySeason_IdAndStatusIn(
                season.getId(),
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        if (pendingOrInProgressTasks > 0 && !Boolean.TRUE.equals(request.getForceComplete())) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        // Set end date
        season.setEndDate(endDate);

        // Calculate actual yield
        if (request.getActualYieldKg() != null) {
            season.setActualYieldKg(request.getActualYieldKg());
        } else {
            syncActualYieldFromHarvests(season);
        }

        season.setStatus(SeasonStatus.COMPLETED);
        Season saved = seasonRepository.save(season);
        return seasonMapper.toResponse(saved);
    }

    /**
     * BR31: Cancel a season: → CANCELLED.
     * Validates no harvests exist unless forceCancel is true.
     */
    public SeasonResponse cancelSeason(Integer id, CancelSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (statusStrategy.isTerminalStatus(season.getStatus())) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        // Check for harvests
        boolean hasHarvests = harvestRepository.existsBySeason_Id(id);
        if (hasHarvests && !Boolean.TRUE.equals(request.getForceCancel())) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        season.setStatus(SeasonStatus.CANCELLED);
        if (season.getEndDate() == null) {
            season.setEndDate(LocalDate.now());
        }

        Season saved = seasonRepository.save(season);
        return seasonMapper.toResponse(saved);
    }

    /**
     * BR15: ArchiveSeason - Archives a completed or cancelled season.
     */
    public SeasonResponse archiveSeason(Integer id) {
        Season season = findAndValidateAccess(id);

        // BR15: Only COMPLETED or CANCELLED seasons can be archived
        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.ARCHIVED)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        season.setStatus(SeasonStatus.ARCHIVED);
        Season saved = seasonRepository.save(season);
        return seasonMapper.toResponse(saved);
    }

    /**
     * BR22/BR26/BR30: ValidateStatusConstraints - Check if status transition is
     * valid.
     */
    public boolean validateStatusConstraints(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        return statusStrategy.canTransition(currentStatus, targetStatus);
    }

    // =========================================================================
    // PASCALCASE WRAPPER METHODS (for BR compliance)
    // =========================================================================

    public SeasonResponse StartSeason(Integer id, StartSeasonRequest request) {
        return startSeason(id, request);
    }

    public SeasonResponse CompleteSeason(Integer id, CompleteSeasonRequest request) {
        return completeSeason(id, request);
    }

    public SeasonResponse CancelSeason(Integer id, CancelSeasonRequest request) {
        return cancelSeason(id, request);
    }

    public SeasonResponse ArchiveSeason(Integer id) {
        return archiveSeason(id);
    }

    public boolean ValidateStatusConstraints(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        return validateStatusConstraints(currentStatus, targetStatus);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Season findAndValidateAccess(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private void syncActualYieldFromHarvests(Season season) {
        var harvests = harvestRepository.findAllBySeason_Id(season.getId());
        if (harvests != null && !harvests.isEmpty()) {
            season.setActualYieldKg(
                    harvests.stream()
                            .map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    }
}
