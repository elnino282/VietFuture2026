package org.example.adminreporting.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.PlotResponse;
import org.example.adminreporting.dto.response.SeasonResponse;
import org.example.adminreporting.entity.FarmSummary;
import org.example.adminreporting.entity.PlotSummary;
import org.example.adminreporting.entity.SeasonSummary;
import org.example.adminreporting.repository.FarmSummaryRepository;
import org.example.adminreporting.repository.PlotSummaryRepository;
import org.example.adminreporting.repository.SeasonSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/plots")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlotController {

    private final PlotSummaryRepository plotSummaryRepository;
    private final FarmSummaryRepository farmSummaryRepository;
    private final SeasonSummaryRepository seasonSummaryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlotResponse>>> listAllPlots(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("Admin requesting all plots from reporting service, farmId: {}, keyword: {}", farmId, keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("plotId").ascending());
        Page<PlotSummary> plotPage;

        if (farmId != null && keyword != null && !keyword.isBlank()) {
            plotPage = plotSummaryRepository.findByFarmIdAndPlotNameContainingIgnoreCase(farmId, keyword, pageable);
        } else if (farmId != null) {
            plotPage = plotSummaryRepository.findByFarmId(farmId, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            plotPage = plotSummaryRepository.findByPlotNameContainingIgnoreCase(keyword, pageable);
        } else {
            plotPage = plotSummaryRepository.findAll(pageable);
        }

        // Pre-fetch farm names to avoid N+1 queries
        List<Integer> farmIds = plotPage.getContent().stream().map(PlotSummary::getFarmId).distinct().toList();
        Map<Integer, String> farmNameMap = farmSummaryRepository.findAllById(farmIds).stream()
                .collect(Collectors.toMap(FarmSummary::getFarmId, FarmSummary::getFarmName));

        Page<PlotResponse> responsePage = plotPage.map(plot -> toPlotResponse(plot, farmNameMap.get(plot.getFarmId())));
        PageResponse<PlotResponse> pageResponse = PageResponse.of(responsePage, responsePage.getContent());

        return ResponseEntity.ok(ApiResponse.success("Plots retrieved", pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlotResponse>> getPlot(@PathVariable Integer id) {
        log.info("Admin requesting plot detail for plot ID: {}", id);
        PlotSummary plot = plotSummaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plot not found with ID: " + id));

        String farmName = farmSummaryRepository.findById(plot.getFarmId())
                .map(FarmSummary::getFarmName)
                .orElse("Unknown Farm");

        return ResponseEntity.ok(ApiResponse.success("Plot retrieved", toPlotResponse(plot, farmName)));
    }

    @GetMapping("/{id}/seasons")
    public ResponseEntity<ApiResponse<List<SeasonResponse>>> listPlotSeasons(@PathVariable Integer id) {
        log.info("Admin requesting seasons for plot ID: {}", id);
        List<SeasonSummary> seasons = seasonSummaryRepository.findByPlotId(id);
        List<SeasonResponse> responseList = seasons.stream().map(this::toSeasonResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Seasons retrieved", responseList));
    }

    private PlotResponse toPlotResponse(PlotSummary plot, String farmName) {
        return PlotResponse.builder()
                .id(plot.getPlotId())
                .farmId(plot.getFarmId())
                .farmName(farmName != null ? farmName : "Farm " + plot.getFarmId())
                .plotName(plot.getPlotName())
                .area(plot.getArea())
                .status("ACTIVE") // Default status for reporting view
                .build();
    }

    private SeasonResponse toSeasonResponse(SeasonSummary season) {
        return SeasonResponse.builder()
                .id(season.getSeasonId())
                .seasonName(season.getSeasonName())
                .plotId(season.getPlotId())
                .cropId(season.getCropId())
                .varietyId(season.getVarietyId())
                .startDate(season.getStartDate())
                .status(season.getStatus())
                .expectedYieldKg(season.getExpectedYieldKg())
                .actualYieldKg(season.getActualYieldKg())
                .build();
    }
}
