package org.example.QuanLyMuaVu.module.season.service;

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
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.season.dto.response.MySeasonResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.SeasonMapper;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
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
public class SeasonQueryService implements SeasonQueryPort {

    SeasonRepository seasonRepository;
    FieldLogRepository fieldLogRepository;
    SeasonMapper seasonMapper;
    FarmAccessPort farmAccessService;

    @Override
    public Optional<Season> findSeasonById(Integer seasonId) {
        if (seasonId == null) {
            return Optional.empty();
        }
        return seasonRepository.findById(seasonId);
    }

    @Override
    public List<Season> findAllSeasonsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return seasonRepository.findAllByFarmUserId(ownerId);
    }

    @Override
    public List<Season> findActiveSeasonsByOwnerIdOrderByStartDateDesc(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return seasonRepository.findActiveSeasonsByUserIdOrderByStartDateDesc(ownerId);
    }

    @Override
    public List<Season> findAllSeasonsByPlotId(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlot_Id(plotId);
    }

    @Override
    public List<Season> findAllSeasonsByFarmIds(Iterable<Integer> farmIds) {
        if (farmIds == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlot_Farm_IdIn(farmIds);
    }

    @Override
    public List<Season> findAllSeasonsByPlotIdOrderByStartDateDesc(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlot_IdOrderByStartDateDesc(plotId);
    }

    @Override
    public List<Season> findAllSeasonsByPlotIdOrderByStartDateAsc(Integer plotId) {
        if (plotId == null) {
            return List.of();
        }
        return seasonRepository.findAllByPlot_IdOrderByStartDateAsc(plotId);
    }

    @Override
    public List<Season> findAllSeasonsByFilters(
            LocalDate from,
            LocalDate to,
            Integer cropId,
            Integer farmId,
            Integer plotId,
            Integer varietyId) {
        return seasonRepository.findByFilters(from, to, cropId, farmId, plotId, varietyId);
    }

    @Override
    public List<Season> findAllSeasons() {
        return seasonRepository.findAll();
    }

    @Override
    public long countSeasonsByStatusAndOwnerId(SeasonStatus status, Long ownerId) {
        if (status == null || ownerId == null) {
            return 0L;
        }
        return seasonRepository.countByStatusAndFarmUserId(status, ownerId);
    }

    @Override
    public long countSeasons() {
        return seasonRepository.count();
    }

    @Override
    public boolean existsSeasonByVarietyId(Integer varietyId) {
        if (varietyId == null) {
            return false;
        }
        return seasonRepository.existsByVariety_Id(varietyId);
    }

    @Override
    public long countFieldLogsBySeasonAndLogType(Integer seasonId, String logType) {
        if (seasonId == null || logType == null || logType.isBlank()) {
            return 0L;
        }
        return fieldLogRepository.countBySeason_IdAndLogTypeIgnoreCase(seasonId, logType);
    }

    public List<MySeasonResponse> getMySeasons() {
        Long currentUserId = farmAccessService.getCurrentUserId();
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        LinkedHashSet<Season> allSeasons = new LinkedHashSet<>();
        if (!accessibleFarmIds.isEmpty()) {
            allSeasons.addAll(seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds));
        }
        allSeasons.addAll(seasonRepository.findAllByPlot_User_Id(currentUserId));

        return allSeasons.stream()
                .sorted((s1, s2) -> Integer.compare(
                        s2.getId() != null ? s2.getId() : 0,
                        s1.getId() != null ? s1.getId() : 0))
                .map(season -> MySeasonResponse.builder()
                        .seasonId(season.getId())
                        .seasonName(season.getSeasonName())
                        .startDate(season.getStartDate())
                        .endDate(season.getEndDate())
                        .plannedHarvestDate(season.getPlannedHarvestDate())
                        .status(season.getStatus() != null ? season.getStatus().name() : null)
                        .build())
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

        Long currentUserId = farmAccessService.getCurrentUserId();
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        List<Season> all = new ArrayList<>();
        if (!accessibleFarmIds.isEmpty()) {
            all.addAll(seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds));
        }
        all.addAll(seasonRepository.findAllByPlot_User_Id(currentUserId));

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
                .filter(season -> plotIdFilter == null
                        || (season.getPlot() != null && plotIdFilter.equals(season.getPlot().getId())))
                .filter(season -> cropIdFilter == null
                        || (season.getCrop() != null && cropIdFilter.equals(season.getCrop().getId())))
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
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return seasonMapper.toDetailResponse(season);
    }

    public Season getSeasonById(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    public List<SeasonResponse> searchSeasonsByKeyword(String keyword) {
        Long currentUserId = farmAccessService.getCurrentUserId();
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
