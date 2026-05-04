package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.LogType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.FieldLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonMinimalResponse;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.example.QuanLyMuaVu.module.season.entity.Season;
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
@Transactional
public class FieldLogService {

    FieldLogRepository fieldLogRepository;
    SeasonRepository seasonRepository;
    FarmAccessPort farmAccessService;

    public PageResponse<FieldLogResponse> listFieldLogsForSeason(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            String type,
            String searchQuery,
            int page,
            int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);

        List<FieldLog> all = fieldLogRepository.findAllBySeason_Id(season.getId());

        String typeFilter = type != null ? type.trim().toLowerCase() : null;
        String queryFilter = searchQuery != null && searchQuery.length() >= 2 ? searchQuery.toLowerCase() : null;

        List<FieldLogResponse> items = all.stream()
                .filter(log -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = log.getLogDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(log -> {
                    if (typeFilter == null || typeFilter.isBlank()) {
                        return true;
                    }
                    return log.getLogType() != null
                            && log.getLogType().toLowerCase().contains(typeFilter);
                })
                .filter(log -> {
                    if (queryFilter == null) {
                        return true;
                    }
                    return log.getNotes() != null && log.getNotes().toLowerCase().contains(queryFilter);
                })
                .sorted((l1, l2) -> Integer.compare(
                        l2.getId() != null ? l2.getId() : 0,
                        l1.getId() != null ? l1.getId() : 0))
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<FieldLogResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<FieldLogResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    public FieldLogResponse createFieldLog(Integer seasonId, CreateFieldLogRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLogs(season, true);

        validateLogType(request.getLogType());
        validateLogDateWithinSeason(season, request.getLogDate());

        FieldLog log = FieldLog.builder()
                .season(season)
                .logDate(request.getLogDate())
                .logType(request.getLogType().toUpperCase().trim())
                .notes(request.getNotes())
                .build();

        FieldLog saved = fieldLogRepository.save(log);
        return toResponse(saved);
    }

    public FieldLogResponse getFieldLog(Integer id) {
        FieldLog log = getFieldLogForCurrentFarmer(id);
        return toResponse(log);
    }

    public FieldLogResponse updateFieldLog(Integer id, UpdateFieldLogRequest request) {
        FieldLog log = getFieldLogForCurrentFarmer(id);
        ensureSeasonOpenForLogs(log.getSeason(), false);

        validateLogType(request.getLogType());
        validateLogDateWithinSeason(log.getSeason(), request.getLogDate());

        log.setLogDate(request.getLogDate());
        log.setLogType(request.getLogType().toUpperCase().trim());
        log.setNotes(request.getNotes());

        FieldLog saved = fieldLogRepository.save(log);
        return toResponse(saved);
    }

    public void deleteFieldLog(Integer id) {
        FieldLog log = getFieldLogForCurrentFarmer(id);
        ensureSeasonOpenForLogs(log.getSeason(), false);

        fieldLogRepository.delete(log);
    }

    private void ensureSeasonOpenForLogs(Season season, boolean forCreate) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            if (forCreate) {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_ADD_FIELD_LOG);
            } else {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_FIELD_LOG);
            }
        }
    }

    private void validateLogType(String logType) {
        if (!LogType.isValid(logType)) {
            throw new AppException(ErrorCode.INVALID_LOG_TYPE);
        }
    }

    private void validateLogDateWithinSeason(Season season, LocalDate date) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();

        if (start == null || date.isBefore(start) || (end != null && date.isAfter(end))) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    private FieldLog getFieldLogForCurrentFarmer(Integer id) {
        FieldLog log = fieldLogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FIELD_LOG_NOT_FOUND));

        Season season = log.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return log;
    }

    private Season getSeasonForCurrentFarmer(Integer id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    /**
     * Get farmer's seasons for dropdown (minimal data).
     */
    @Transactional(readOnly = true)
    public List<SeasonMinimalResponse> getMySeasons() {
        List<Integer> accessibleFarmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();

        return seasonRepository.findAllByPlot_Farm_IdIn(accessibleFarmIds).stream()
                .map(season -> SeasonMinimalResponse.builder()
                        .seasonId(season.getId())
                        .seasonName(season.getSeasonName())
                        .startDate(season.getStartDate())
                        .endDate(season.getEndDate())
                        .plannedHarvestDate(season.getPlannedHarvestDate())
                        .build())
                .toList();
    }

    private FieldLogResponse toResponse(FieldLog log) {
        return FieldLogResponse.builder()
                .id(log.getId())
                .seasonId(log.getSeason() != null ? log.getSeason().getId() : null)
                .seasonName(log.getSeason() != null ? log.getSeason().getSeasonName() : null)
                .logDate(log.getLogDate())
                .logType(log.getLogType())
                .notes(log.getNotes())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
