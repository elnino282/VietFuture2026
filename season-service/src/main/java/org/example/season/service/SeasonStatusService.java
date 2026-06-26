package org.example.season.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.enums.SeasonStatus;
import org.example.season.enums.TaskStatus;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.strategy.SeasonStatusStrategy;
import org.example.season.dto.request.CancelSeasonRequest;
import org.example.season.dto.request.CompleteSeasonRequest;
import org.example.season.dto.request.StartSeasonRequest;
import org.example.season.dto.request.UpdateSeasonStatusRequest;
import org.example.season.dto.response.SeasonResponse;
import org.example.season.entity.Season;
import org.example.season.event.DomainEventPublisher;
import org.example.season.event.SeasonChangedEvent;
import org.example.season.mapper.SeasonMapper;
import org.example.season.repository.HarvestRepository;
import org.example.season.repository.SeasonRepository;
import org.example.season.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonStatusService {

    SeasonRepository seasonRepository;
    HarvestRepository harvestRepository;
    TaskRepository taskRepository;
    SeasonMapper seasonMapper;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;
    ExternalServiceClient externalServiceClient;
    SeasonStatusStrategy statusStrategy;
    DomainEventPublisher domainEventPublisher;

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

        if (targetStatus == SeasonStatus.COMPLETED || targetStatus == SeasonStatus.ARCHIVED) {
            syncActualYieldFromHarvests(season);
        }

        Season saved = seasonRepository.save(season);
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.STATUS_CHANGED));
        return seasonMapper.toResponse(saved);
    }

    public SeasonResponse startSeason(Integer id, StartSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.ACTIVE)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        if (season.getPlotId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }

        if (plot.getFarmActive() == null || !plot.getFarmActive()) {
            throw new AppException(ErrorCode.FARM_INACTIVE);
        }

        if (request != null && request.getActualStartDate() != null) {
            season.setStartDate(request.getActualStartDate());
        }

        season.setStatus(SeasonStatus.ACTIVE);
        Season saved = seasonRepository.save(season);
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot.getFarmId(), SeasonChangedEvent.Action.STATUS_CHANGED));
        return seasonMapper.toResponse(saved);
    }

    public SeasonResponse completeSeason(Integer id, CompleteSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.COMPLETED)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        LocalDate endDate = request.getEndDate();
        if (endDate.isBefore(season.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }

        long pendingOrInProgressTasks = taskRepository.countBySeasonIdAndStatusIn(
                season.getId(),
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        if (pendingOrInProgressTasks > 0 && !Boolean.TRUE.equals(request.getForceComplete())) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        season.setEndDate(endDate);

        if (request.getActualYieldKg() != null) {
            season.setActualYieldKg(request.getActualYieldKg());
        } else {
            syncActualYieldFromHarvests(season);
        }

        season.setStatus(SeasonStatus.COMPLETED);
        Season saved = seasonRepository.save(season);
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.COMPLETED));
        return seasonMapper.toResponse(saved);
    }

    public SeasonResponse cancelSeason(Integer id, CancelSeasonRequest request) {
        Season season = findAndValidateAccess(id);

        if (statusStrategy.isTerminalStatus(season.getStatus())) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        boolean hasHarvests = harvestRepository.existsBySeasonId(id);
        if (hasHarvests && !Boolean.TRUE.equals(request.getForceCancel())) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        season.setStatus(SeasonStatus.CANCELLED);
        if (season.getEndDate() == null) {
            season.setEndDate(LocalDate.now());
        }

        Season saved = seasonRepository.save(season);
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.STATUS_CHANGED));
        return seasonMapper.toResponse(saved);
    }

    public SeasonResponse archiveSeason(Integer id) {
        Season season = findAndValidateAccess(id);

        if (!statusStrategy.canTransition(season.getStatus(), SeasonStatus.ARCHIVED)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        season.setStatus(SeasonStatus.ARCHIVED);
        Season saved = seasonRepository.save(season);
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.STATUS_CHANGED));
        return seasonMapper.toResponse(saved);
    }

    public boolean validateStatusConstraints(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        return statusStrategy.canTransition(currentStatus, targetStatus);
    }

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

    private Season findAndValidateAccess(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private void syncActualYieldFromHarvests(Season season) {
        var harvests = harvestRepository.findAllBySeasonId(season.getId());
        if (harvests != null && !harvests.isEmpty()) {
            season.setActualYieldKg(
                    harvests.stream()
                            .map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    }
}
