package org.example.adminreporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.SeasonDetailResponse;
import org.example.adminreporting.dto.response.SeasonResponse;
import org.example.adminreporting.entity.PlotSummary;
import org.example.adminreporting.entity.SeasonSummary;
import org.example.adminreporting.repository.PlotSummaryRepository;
import org.example.adminreporting.repository.SeasonSummaryRepository;
import org.example.adminreporting.repository.TaskSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/seasons")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeasonController {

    private final SeasonSummaryRepository seasonSummaryRepository;
    private final PlotSummaryRepository plotSummaryRepository;
    private final TaskSummaryRepository taskSummaryRepository;
    private final org.example.adminreporting.mapper.AdminReportingMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeasonResponse>>> listAllSeasons(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("Admin requesting all seasons from reporting service, farmId={}, status={}, cropId={}, plotId={}",
                farmId, status, cropId, plotId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("seasonId").descending());
        Page<SeasonSummary> seasonPage = seasonSummaryRepository.findSeasonsWithFilters(farmId, status, cropId, plotId, pageable);

        Page<SeasonResponse> responsePage = seasonPage.map(mapper::toSeasonResponse);
        PageResponse<SeasonResponse> pageResponse = PageResponse.of(responsePage, responsePage.getContent());

        return ResponseEntity.ok(ApiResponse.success("Seasons retrieved", pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeasonDetailResponse>> getSeasonDetail(@PathVariable Integer id) {
        log.info("Admin requesting season detail for season ID: {}", id);
        SeasonSummary season = seasonSummaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Season not found with ID: " + id));

        String plotName = plotSummaryRepository.findById(season.getPlotId())
                .map(PlotSummary::getPlotName)
                .orElse("Plot " + season.getPlotId());

        SeasonDetailResponse detail = mapper.toSeasonDetailResponse(season, plotName);

        return ResponseEntity.ok(ApiResponse.success("Season detail retrieved", detail));
    }

    @GetMapping("/{id}/pending-task-count")
    public ResponseEntity<ApiResponse<Long>> getPendingTaskCount(@PathVariable Integer id) {
        log.info("Admin requesting pending task count for season ID: {}", id);
        long pendingCount = taskSummaryRepository.countPendingTasks(id);
        return ResponseEntity.ok(ApiResponse.success("Pending task count retrieved", pendingCount));
    }

}
