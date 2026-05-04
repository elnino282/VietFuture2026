package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.SeasonCreatedEvent;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.dto.request.CancelSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CompleteSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.StartSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonStatusRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.MySeasonResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.SeasonMapper;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core Season CRUD service.
 * Refactored to follow Single Responsibility Principle.
 * 
 * Responsibilities split into:
 * - SeasonService (this): CRUD operations only
 * - SeasonQueryService: Search and query operations
 * - SeasonStatusService: Status transitions
 * - SeasonValidationService: Business rule validation
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonService {

    // Repositories
    SeasonRepository seasonRepository;
    FarmQueryPort farmQueryPort;
    HarvestRepository harvestRepository;
    ExpenseQueryPort expenseQueryPort;
    TaskRepository taskRepository;
    FieldLogRepository fieldLogRepository;
    CropCatalogQueryPort cropCatalogQueryPort;

    // Mappers
    SeasonMapper seasonMapper;

    // Delegated Services (SRP compliance)
    SeasonQueryService queryService;
    SeasonStatusService statusService;
    SeasonValidationService validationService;
    FarmAccessPort farmAccessService;
    DomainEventPublisher domainEventPublisher;

    // =========================================================================
    // LEGACY METHODS (Backward Compatibility)
    // =========================================================================

    /**
     * Legacy creation method kept for backward compatibility.
     */
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

    /**
     * Legacy list method, returns all seasons (no filtering by user).
     */
    public List<Season> getAll() {
        return seasonRepository.findAll();
    }

    public Season getById(Integer id) {
        return seasonRepository.findById(id).orElse(null);
    }

    /**
     * Legacy update method kept for backward compatibility.
     */
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

    // =========================================================================
    // DELEGATED QUERY METHODS
    // =========================================================================

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

    // =========================================================================
    // DELEGATED STATUS METHODS
    // =========================================================================

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

    // PascalCase wrappers for BR compliance
    public SeasonResponse StartSeason(Integer id, StartSeasonRequest request) {
        return statusService.StartSeason(id, request);
    }

    public SeasonResponse CompleteSeason(Integer id, CompleteSeasonRequest request) {
        return statusService.CompleteSeason(id, request);
    }

    public SeasonResponse CancelSeason(Integer id, CancelSeasonRequest request) {
        return statusService.CancelSeason(id, request);
    }

    // =========================================================================
    // CRUD OPERATIONS (Core responsibility)
    // =========================================================================

    public SeasonDetailResponse createSeason(CreateSeasonRequest request) {
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = farmQueryPort.findPlotById(request.getPlotId())
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessPlot(plot);

        org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop = cropCatalogQueryPort.findCropById(request.getCropId())
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));

        org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety = null;
        if (request.getVarietyId() != null) {
            variety = cropCatalogQueryPort.findVarietyById(request.getVarietyId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
            if (!variety.getCrop().getId().equals(crop.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        // Delegate validation
        validationService.validateSeasonDates(
                request.getStartDate(), request.getEndDate(), request.getPlannedHarvestDate());
        validationService.validateNoOverlappingActiveOrPlannedSeasons(
                plot, request.getStartDate(), request.getPlannedHarvestDate(), request.getEndDate(), null);

        Season season = Season.builder()
                .plot(plot)
                .crop(crop)
                .variety(variety)
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
        domainEventPublisher.publish(new SeasonCreatedEvent(saved));
        return seasonMapper.toDetailResponse(saved);
    }

    /**
     * BR103: CreateSeason(Season season) - Creates a new season with full
     * validation.
     * Uses ValidateDataFormat() for input validation and checks DB constraints (MSG
     * 9).
     */
    public SeasonDetailResponse CreateSeason(CreateSeasonRequest request) {
        // BR102: Call ValidateDataFormat() for input validation (MSG_1, MSG_4)
        validationService.ValidateDataFormat(
                request.getSeasonName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPlotId(),
                request.getDescription());

        // BR103: Check season name uniqueness (MSG 9)
        validationService.validateSeasonNameUniquenessInPlot(request.getPlotId(), request.getSeasonName(), null);
        return createSeason(request);
    }

    public SeasonDetailResponse updateSeason(Integer id, UpdateSeasonRequest request) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);

        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }

        // Delegate validation
        validationService.validateSeasonDates(
                request.getStartDate(), request.getEndDate(), request.getPlannedHarvestDate());
        validationService.validateNoOverlappingActiveOrPlannedSeasons(
                season.getPlot(),
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
            org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety = cropCatalogQueryPort.findVarietyById(request.getVarietyId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
            if (!variety.getCrop().getId().equals(season.getCrop().getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            season.setVariety(variety);
        }

        Season saved = seasonRepository.save(season);
        return seasonMapper.toDetailResponse(saved);
    }

    /**
     * BR107: UpdateSeason(Season season) - Updates a season with full validation.
     * Uses ValidateDataFormat() for input validation and checks DB constraints (MSG
     * 9).
     */
    public SeasonDetailResponse UpdateSeason(Integer id, UpdateSeasonRequest request) {
        Season existing = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        // BR106: Call ValidateDataFormat() for input validation (MSG_1, MSG_4)
        validationService.ValidateDataFormat(
                request.getSeasonName(),
                request.getStartDate(),
                request.getEndDate(),
                existing.getPlot().getId(),
                request.getDescription());

        // BR107: Check season name uniqueness (MSG 9)
        validationService.validateSeasonNameUniquenessInPlot(
                existing.getPlot().getId(), request.getSeasonName(), id);
        return updateSeason(id, request);
    }

    public void deleteSeason(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);

        if (season.getStatus() != SeasonStatus.PLANNED) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        boolean hasTasks = taskRepository.existsBySeason_Id(id);
        boolean hasFieldLogs = fieldLogRepository.existsBySeason_Id(id);
        boolean hasHarvests = harvestRepository.existsBySeason_Id(id);
        boolean hasExpenses = expenseQueryPort.existsExpenseBySeasonId(id);

        if (hasTasks || hasFieldLogs || hasHarvests || hasExpenses) {
            throw new AppException(ErrorCode.SEASON_HAS_CHILD_RECORDS);
        }

        seasonRepository.delete(season);
    }
}

