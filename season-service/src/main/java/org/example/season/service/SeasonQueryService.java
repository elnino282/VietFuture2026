package org.example.season.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.dto.common.PageResponse;
import org.example.season.enums.SeasonStatus;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.dto.response.MySeasonResponse;
import org.example.season.dto.response.SeasonDetailResponse;
import org.example.season.dto.response.SeasonResponse;
import org.example.season.entity.Season;
import org.example.season.mapper.SeasonMapper;
import org.example.season.repository.FieldLogRepository;
import org.example.season.repository.SeasonRepository;
import org.example.season.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SeasonQueryService {

    SeasonRepository seasonRepository;
    FieldLogRepository fieldLogRepository;
    SeasonMapper seasonMapper;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;
    ExternalServiceClient externalServiceClient;
    TaskRepository taskRepository;

    private static final List<SeasonStatus> BLOCKING_SEASON_STATUSES = List.of(
            SeasonStatus.PLANNED,
            SeasonStatus.ACTIVE);
    private static final List<org.example.season.enums.TaskStatus> BLOCKING_TASK_STATUSES = List.of(
            org.example.season.enums.TaskStatus.PENDING,
            org.example.season.enums.TaskStatus.IN_PROGRESS,
            org.example.season.enums.TaskStatus.OVERDUE);

    public Optional<Season> findSeasonById(Integer seasonId) {
        if (seasonId == null) {
            return Optional.empty();
        }
        return seasonRepository.findById(seasonId);
    }

    public List<Season> findAllSeasonsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return seasonRepository.findAllByFarmUserId(ownerId);
    }

    public List<Season> findActiveSeasonsByOwnerIdOrderByStartDateDesc(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return seasonRepository.findActiveSeasonsByUserIdOrderByStartDateDesc(ownerId);
    }

    public List<Season> findAllSeasonsByPlotId(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlotId(plotId);
    }

    public List<Season> findAllSeasonsByFarmIds(Iterable<Integer> farmIds) {
        if (farmIds == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlotFarmIdIn(farmIds);
    }

    public List<Season> findAllSeasonsByPlotIdOrderByStartDateDesc(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlotIdOrderByStartDateDesc(plotId);
    }

    public List<Season> findAllSeasonsByPlotIdOrderByStartDateAsc(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlotIdOrderByStartDateAsc(plotId);
    }

    public List<Season> findAllSeasonsByFilters(
            LocalDate from,
            LocalDate to,
            Integer cropId,
            Integer farmId,
            Integer plotId,
            Integer varietyId) {
        return seasonRepository.findByFilters(from, to, cropId, farmId, plotId, varietyId);
    }

    public List<Season> findAllSeasons() {
        return seasonRepository.findAll();
    }

    public long countSeasonsByStatusAndOwnerId(SeasonStatus status, Long ownerId) {
        if (status == null || ownerId == null) {
            return 0L;
        }
        return seasonRepository.countByStatusAndFarmUserId(status.name(), ownerId);
    }

    public long countSeasons() {
        return seasonRepository.count();
    }

    public boolean existsSeasonByVarietyId(Integer varietyId) {
        if (varietyId == null) {
            return false;
        }
        return seasonRepository.existsByVarietyId(varietyId);
    }

    public boolean existsActiveSeasonByPlotId(Integer plotId) {
        if (plotId == null) {
            return false;
        }
        return seasonRepository.existsByPlotIdAndStatusIn(plotId, BLOCKING_SEASON_STATUSES);
    }

    public boolean existsActiveTasksByPlotId(Integer plotId) {
        if (plotId == null) {
            return false;
        }
        return taskRepository.existsBySeasonPlotIdAndStatusIn(plotId, BLOCKING_TASK_STATUSES);
    }

    public long countFieldLogsBySeasonAndLogType(Integer seasonId, String logType) {
        if (seasonId == null || logType == null || logType.isBlank()) {
            return 0L;
        }
        return fieldLogRepository.countBySeasonIdAndLogTypeIgnoreCase(seasonId, logType);
    }

    public List<MySeasonResponse> getMySeasons() {
        Long currentUserId = seasonWorkspaceAccessService.getCurrentUserId();

        LinkedHashSet<Season> allSeasons = new LinkedHashSet<>();
        allSeasons.addAll(seasonRepository.findAllByFarmUserId(currentUserId));
        allSeasons.addAll(seasonRepository.findAllByPlotUserId(currentUserId));

        return allSeasons.stream()
                .sorted((s1, s2) -> Integer.compare(
                        s2.getId() != null ? s2.getId() : 0,
                        s1.getId() != null ? s1.getId() : 0))
                .map(season -> {
                    ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
                    return MySeasonResponse.builder()
                            .seasonId(season.getId())
                            .seasonName(season.getSeasonName())
                            .farmId(plot != null ? plot.getFarmId() : null)
                            .farmName(plot != null ? plot.getFarmName() : null)
                            .plotId(season.getPlotId())
                            .plotName(plot != null ? plot.getPlotName() : null)
                            .startDate(season.getStartDate())
                            .endDate(season.getEndDate())
                            .plannedHarvestDate(season.getPlannedHarvestDate())
                            .status(season.getStatus() != null ? season.getStatus().name() : null)
                            .build();
                })
                .toList();
    }

    public PageResponse<SeasonResponse> searchMySeasons(
            Integer plotId,
            Integer cropId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        Long currentUserId = seasonWorkspaceAccessService.getCurrentUserId();

        List<Season> all = new ArrayList<>();
        all.addAll(seasonRepository.findAllByFarmUserId(currentUserId));
        all.addAll(seasonRepository.findAllByPlotUserId(currentUserId));

        Map<Integer, Season> byId = new LinkedHashMap<>();
        for (Season season : all) {
            if (season.getId() != null) {
                byId.putIfAbsent(season.getId(), season);
            }
        }
        all = new ArrayList<>(byId.values());

        SeasonStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = SeasonStatus.fromCode(status);
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        final Integer plotIdFilter = plotId;
        final Integer cropIdFilter = cropId;
        final SeasonStatus statusFilterFinal = statusFilter;
        final LocalDate fromDate = from;
        final LocalDate toDate = to;

        List<SeasonResponse> filtered = all.stream()
                .filter(season -> plotIdFilter == null || plotIdFilter.equals(season.getPlotId()))
                .filter(season -> cropIdFilter == null || cropIdFilter.equals(season.getCropId()))
                .filter(season -> statusFilterFinal == null || statusFilterFinal.equals(season.getStatus()))
                .filter(season -> filterByDateRange(season, fromDate, toDate))
                .sorted((s1, s2) -> Integer.compare(
                        s2.getId() != null ? s2.getId() : 0,
                        s1.getId() != null ? s1.getId() : 0))
                .map(seasonMapper::toResponse)
                .toList();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<SeasonResponse> pageItems = fromIndex >= filtered.size()
                ? List.of()
                : filtered.subList(fromIndex, toIndex);
        Page<SeasonResponse> pageData = new PageImpl<>(pageItems, pageable, filtered.size());

        return PageResponse.of(pageData, pageItems);
    }

    public SeasonDetailResponse getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);
        return seasonMapper.toDetailResponse(season);
    }

    public Season getSeasonById(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    public List<SeasonResponse> searchSeasonsByKeyword(String keyword) {
        Long currentUserId = seasonWorkspaceAccessService.getCurrentUserId();
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return seasonRepository.searchByKeywordAndUserId(keyword.trim(), currentUserId)
                .stream()
                .map(seasonMapper::toResponse)
                .toList();
    }

    private boolean filterByDateRange(Season season, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate seasonStart = season.getStartDate();
        LocalDate seasonEnd = season.getEndDate() != null ? season.getEndDate() : seasonStart;
        LocalDate rangeStart = fromDate != null ? fromDate : seasonStart;
        LocalDate rangeEnd = toDate != null ? toDate : seasonEnd;
        return !seasonEnd.isBefore(rangeStart) && !seasonStart.isAfter(rangeEnd);
    }
}
