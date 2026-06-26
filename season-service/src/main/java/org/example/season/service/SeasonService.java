package org.example.season.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.event.DomainEventPublisher;
import org.example.season.event.SeasonChangedEvent;
import org.example.season.dto.common.PageResponse;
import org.example.season.enums.SeasonStatus;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.dto.request.CancelSeasonRequest;
import org.example.season.dto.request.CompleteSeasonRequest;
import org.example.season.dto.request.CreateSeasonRequest;
import org.example.season.dto.request.StartSeasonRequest;
import org.example.season.dto.request.UpdateSeasonRequest;
import org.example.season.dto.request.UpdateSeasonStatusRequest;
import org.example.season.dto.response.MySeasonResponse;
import org.example.season.dto.response.SeasonDetailResponse;
import org.example.season.dto.response.SeasonResponse;
import org.example.season.entity.Season;
import org.example.season.mapper.SeasonMapper;
import org.example.season.repository.FieldLogRepository;
import org.example.season.repository.HarvestRepository;
import org.example.season.repository.SeasonRepository;
import org.example.season.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonService {

    SeasonRepository seasonRepository;
    ExternalServiceClient externalServiceClient;
    HarvestRepository harvestRepository;
    TaskRepository taskRepository;
    FieldLogRepository fieldLogRepository;

    SeasonMapper seasonMapper;

    SeasonQueryService queryService;
    SeasonStatusService statusService;
    SeasonValidationService validationService;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;
    DomainEventPublisher domainEventPublisher;

    public Season create(Integer plotId, Integer cropId, String seasonName, LocalDate startDate,
            Integer initialPlantCount) {
        CreateSeasonRequest request = CreateSeasonRequest.builder()
                .plotId(plotId)
                .cropId(cropId)
                .seasonName(seasonName)
                .startDate(startDate)
                .initialPlantCount(initialPlantCount)
                .build();
        SeasonDetailResponse created = createSeason(request);
        return seasonRepository.findById(created.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
    }

    public List<Season> getAll() {
        return seasonRepository.findAll();
    }

    public Season getById(Integer id) {
        return seasonRepository.findById(id).orElse(null);
    }

    public Season update(Integer id, String seasonName, LocalDate startDate, Integer currentPlantCount) {
        UpdateSeasonRequest request = UpdateSeasonRequest.builder()
                .seasonName(seasonName)
                .startDate(startDate)
                .currentPlantCount(currentPlantCount)
                .build();
        SeasonDetailResponse updated = updateSeason(id, request);
        return seasonRepository.findById(updated.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
    }

    public void delete(Integer id) {
        deleteSeason(id);
    }

    public List<MySeasonResponse> getMySeasons() {
        return queryService.getMySeasons();
    }

    public PageResponse<SeasonResponse> searchMySeasons(
            Integer plotId, Integer cropId, String status,
            LocalDate from, LocalDate to, int page, int size) {
        return queryService.searchMySeasons(plotId, cropId, status, from, to, page, size);
    }

    public SeasonDetailResponse getSeasonForCurrentFarmer(Integer id) {
        return queryService.getSeasonForCurrentFarmer(id);
    }

    public List<SeasonResponse> searchSeasonsByKeyword(String keyword) {
        return queryService.searchSeasonsByKeyword(keyword);
    }

    public Season getSeasonById(Integer id) {
        return queryService.getSeasonById(id);
    }

    public SeasonResponse updateSeasonStatus(Integer id, UpdateSeasonStatusRequest request) {
        return statusService.updateSeasonStatus(id, request);
    }

    public SeasonResponse startSeason(Integer id, StartSeasonRequest request) {
        return statusService.startSeason(id, request);
    }

    public SeasonResponse completeSeason(Integer id, CompleteSeasonRequest request) {
        return statusService.completeSeason(id, request);
    }

    public SeasonResponse cancelSeason(Integer id, CancelSeasonRequest request) {
        return statusService.cancelSeason(id, request);
    }

    public SeasonResponse ArchiveSeason(Integer id) {
        return statusService.archiveSeason(id);
    }

    public boolean ValidateStatusConstraints(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        return statusService.validateStatusConstraints(currentStatus, targetStatus);
    }

    public SeasonResponse StartSeason(Integer id, StartSeasonRequest request) {
        return statusService.StartSeason(id, request);
    }

    public SeasonResponse CompleteSeason(Integer id, CompleteSeasonRequest request) {
        return statusService.CompleteSeason(id, request);
    }

    public SeasonResponse CancelSeason(Integer id, CancelSeasonRequest request) {
        return statusService.CancelSeason(id, request);
    }

    public SeasonDetailResponse createSeason(CreateSeasonRequest request) {
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(request.getPlotId());
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }
        seasonWorkspaceAccessService.assertCurrentUserCanAccessPlot(request.getPlotId());

        ExternalServiceClient.CropInternalDto crop = externalServiceClient.getCrop(request.getCropId());
        if (crop == null) {
            throw new AppException(ErrorCode.CROP_NOT_FOUND);
        }

        if (request.getVarietyId() != null) {
            ExternalServiceClient.VarietyInternalDto variety = externalServiceClient.getVariety(request.getVarietyId());
            if (variety == null) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if (variety.getCropId() == null || !variety.getCropId().equals(crop.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        validationService.validateSeasonDates(
                request.getStartDate(), request.getEndDate(), request.getPlannedHarvestDate());
        validationService.validateNoOverlappingActiveOrPlannedSeasons(
                request.getPlotId(), request.getStartDate(), request.getPlannedHarvestDate(), request.getEndDate(), null);

        Season season = Season.builder()
                .plotId(request.getPlotId())
                .cropId(request.getCropId())
                .varietyId(request.getVarietyId())
                .seasonName(request.getSeasonName())
                .startDate(request.getStartDate())
                .plannedHarvestDate(request.getPlannedHarvestDate())
                .endDate(request.getEndDate())
                .status(SeasonStatus.PLANNED)
                .initialPlantCount(request.getInitialPlantCount())
                .currentPlantCount(request.getInitialPlantCount())
                .expectedYieldKg(request.getExpectedYieldKg())
                .notes(request.getNotes())
                .build();

        Season saved = seasonRepository.save(season);
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.CREATED));
        return seasonMapper.toDetailResponse(saved);
    }

    public SeasonDetailResponse CreateSeason(CreateSeasonRequest request) {
        validationService.ValidateDataFormat(
                request.getSeasonName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPlotId(),
                request.getDescription());

        validationService.validateSeasonNameUniquenessInPlot(request.getPlotId(), request.getSeasonName(), null);
        return createSeason(request);
    }

    public SeasonDetailResponse updateSeason(Integer id, UpdateSeasonRequest request) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);

        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        validationService.validateSeasonDates(
                request.getStartDate(), request.getEndDate(), request.getPlannedHarvestDate());
        validationService.validateNoOverlappingActiveOrPlannedSeasons(
                season.getPlotId(),
                request.getStartDate(),
                request.getPlannedHarvestDate(),
                request.getEndDate(),
                id);

        season.setSeasonName(request.getSeasonName());
        season.setStartDate(request.getStartDate());
        season.setPlannedHarvestDate(request.getPlannedHarvestDate());
        season.setEndDate(request.getEndDate());
        season.setCurrentPlantCount(request.getCurrentPlantCount());
        season.setExpectedYieldKg(request.getExpectedYieldKg());
        season.setActualYieldKg(request.getActualYieldKg());
        season.setNotes(request.getNotes());

        if (request.getVarietyId() != null) {
            ExternalServiceClient.VarietyInternalDto variety = externalServiceClient.getVariety(request.getVarietyId());
            if (variety == null) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            if (variety.getCropId() == null || !variety.getCropId().equals(season.getCropId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            season.setVarietyId(request.getVarietyId());
        } else {
            season.setVarietyId(null);
        }

        Season saved = seasonRepository.save(season);
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(saved.getPlotId());
        domainEventPublisher.publish(new SeasonChangedEvent(saved, plot != null ? plot.getFarmId() : null, SeasonChangedEvent.Action.UPDATED));
        return seasonMapper.toDetailResponse(saved);
    }

    public SeasonDetailResponse UpdateSeason(Integer id, UpdateSeasonRequest request) {
        Season existing = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        validationService.ValidateDataFormat(
                request.getSeasonName(),
                request.getStartDate(),
                request.getEndDate(),
                existing.getPlotId(),
                request.getDescription());

        validationService.validateSeasonNameUniquenessInPlot(
                existing.getPlotId(), request.getSeasonName(), id);
        return updateSeason(id, request);
    }

    public void deleteSeason(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);

        if (season.getStatus() != SeasonStatus.PLANNED) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        boolean hasTasks = taskRepository.existsBySeasonId(id);
        boolean hasFieldLogs = fieldLogRepository.existsBySeasonId(id);
        boolean hasHarvests = harvestRepository.existsBySeasonId(id);
        boolean hasExpenses = externalServiceClient.existsExpenseBySeasonId(id);

        if (hasTasks || hasFieldLogs || hasHarvests || hasExpenses) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        seasonRepository.delete(season);
    }
}
