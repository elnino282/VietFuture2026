package org.example.QuanLyMuaVu.module.admin.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminSeasonUpdateRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.mapper.SeasonMapper;
import org.example.QuanLyMuaVu.module.season.port.SeasonCommandPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskCommandPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminSeasonService {

    SeasonQueryPort seasonQueryPort;
    SeasonCommandPort seasonCommandPort;
    TaskQueryPort taskQueryPort;
    TaskCommandPort taskCommandPort;
    SeasonMapper seasonMapper;

    public PageResponse<SeasonResponse> getAllSeasons(
            Integer farmId,
            String status,
            Integer cropId,
            Integer plotId,
            int page,
            int size) {
        log.info("Admin fetching all seasons - farmId: {}, status: {}, cropId: {}, plotId: {}, page: {}, size: {}",
                farmId, status, cropId, plotId, page, size);

        SeasonStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = SeasonStatus.fromCode(status);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid season status filter: {}", status);
            }
        }
        final SeasonStatus effectiveStatusFilter = statusFilter;

        List<SeasonResponse> filtered = seasonQueryPort.findAllSeasons().stream()
                .filter(season -> farmId == null
                        || (season.getPlot() != null
                                && season.getPlot().getFarm() != null
                                && farmId.equals(season.getPlot().getFarm().getId())))
                .filter(season -> cropId == null
                        || (season.getCrop() != null && cropId.equals(season.getCrop().getId())))
                .filter(season -> plotId == null
                        || (season.getPlot() != null && plotId.equals(season.getPlot().getId())))
                .filter(season -> effectiveStatusFilter == null || effectiveStatusFilter.equals(season.getStatus()))
                .map(seasonMapper::toResponse)
                .toList();

        return toPagedResponse(filtered, page, size);
    }

    public SeasonDetailResponse getSeasonById(Integer seasonId) {
        log.info("Admin fetching season detail for ID: {}", seasonId);
        org.example.QuanLyMuaVu.module.season.entity.Season season = seasonQueryPort.findSeasonById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        return seasonMapper.toDetailResponse(season);
    }

    public Long getPendingTaskCount(Integer seasonId) {
        return taskQueryPort.countTasksBySeasonIdAndStatusNot(seasonId, TaskStatus.DONE);
    }

    @Transactional
    public SeasonResponse updateSeason(Integer seasonId, AdminSeasonUpdateRequest request) {
        log.info("Admin updating season {} with request: {}", seasonId, request);

        org.example.QuanLyMuaVu.module.season.entity.Season season = seasonQueryPort.findSeasonById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        boolean isCompletingNow = false;
        if (request.getStatus() != null) {
            SeasonStatus newStatus = SeasonStatus.fromCode(request.getStatus());
            if (newStatus == SeasonStatus.COMPLETED && season.getStatus() != SeasonStatus.COMPLETED) {
                isCompletingNow = true;
                if (request.getEndDate() == null || request.getActualYieldKg() == null) {
                    throw new AppException(ErrorCode.SEASON_COMPLETION_REQUIRES_YIELD_AND_DATE);
                }
            }
            season.setStatus(newStatus);
        }

        if (request.getEndDate() != null) {
            season.setEndDate(request.getEndDate());
        }
        if (request.getActualYieldKg() != null) {
            season.setActualYieldKg(request.getActualYieldKg());
        }
        if (request.getNotes() != null) {
            season.setNotes(request.getNotes());
        }

        if (isCompletingNow) {
            List<org.example.QuanLyMuaVu.module.season.entity.Task> pendingTasks = taskQueryPort.findTasksBySeasonIdAndStatusNot(seasonId, TaskStatus.DONE);
            for (org.example.QuanLyMuaVu.module.season.entity.Task task : pendingTasks) {
                task.setStatus(TaskStatus.CANCELLED);
                String existingNotes = task.getNotes() != null ? task.getNotes() + "\n" : "";
                task.setNotes(existingNotes + "Auto-cancelled by org.example.QuanLyMuaVu.module.season.entity.Season Completion (Admin Intervention)");
                taskCommandPort.saveTask(task);
            }
            if (!pendingTasks.isEmpty()) {
                log.info("Admin Intervention: Auto-cancelled {} pending tasks for season {}",
                        pendingTasks.size(), seasonId);
            }
        }

        org.example.QuanLyMuaVu.module.season.entity.Season saved = seasonCommandPort.saveSeason(season);
        return seasonMapper.toResponse(saved);
    }

    private PageResponse<SeasonResponse> toPagedResponse(List<SeasonResponse> allItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = safePage * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, allItems.size());
        List<SeasonResponse> pageItems = fromIndex >= allItems.size() ? List.of() : allItems.subList(fromIndex, toIndex);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return PageResponse.of(new PageImpl<>(pageItems, pageable, allItems.size()), pageItems);
    }
}
