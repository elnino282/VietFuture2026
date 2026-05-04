package org.example.QuanLyMuaVu.module.admin.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.PlotStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.dto.response.PlotResponse;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlotQueryService {

    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;

    public PageResponse<PlotResponse> listAllPlots(Integer farmId, String keyword, int page, int size) {
        List<org.example.QuanLyMuaVu.module.farm.entity.Plot> allPlots = farmId != null
                ? farmQueryPort.findPlotsByFarmId(farmId)
                : farmQueryPort.findAllPlots();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.toLowerCase();
            allPlots = allPlots.stream()
                    .filter(p -> p.getPlotName() != null && p.getPlotName().toLowerCase().contains(keywordLower))
                    .toList();
        }

        int start = page * size;
        int end = Math.min(start + size, allPlots.size());
        List<org.example.QuanLyMuaVu.module.farm.entity.Plot> pageContent = start < allPlots.size() ? allPlots.subList(start, end) : List.of();

        List<PlotResponse> content = pageContent.stream()
                .map(this::toPlotResponse)
                .toList();

        PageResponse<PlotResponse> pageResponse = new PageResponse<>();
        pageResponse.setItems(content);
        pageResponse.setPage(page);
        pageResponse.setSize(size);
        pageResponse.setTotalElements(allPlots.size());
        pageResponse.setTotalPages((int) Math.ceil((double) allPlots.size() / size));
        return pageResponse;
    }

    public PlotResponse getPlot(Integer id) {
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = farmQueryPort.findPlotById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
        return toPlotResponse(plot);
    }

    public List<SeasonResponse> listPlotSeasons(Integer id) {
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = farmQueryPort.findPlotById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        return seasonQueryPort.findAllSeasonsByPlotId(plot.getId()).stream()
                .map(this::toSeasonResponse)
                .toList();
    }

    private PlotResponse toPlotResponse(org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        PlotStatus plotStatus = null;
        if (plot.getStatus() != null) {
            try {
                plotStatus = PlotStatus.valueOf(plot.getStatus());
            } catch (IllegalArgumentException ignored) {
                plotStatus = null;
            }
        }

        return PlotResponse.builder()
                .id(plot.getId())
                .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                .plotName(plot.getPlotName())
                .area(plot.getArea())
                .soilType(plot.getSoilType())
                .status(plotStatus)
                .build();
    }

    private SeasonResponse toSeasonResponse(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        return SeasonResponse.builder()
                .id(season.getId())
                .seasonName(season.getSeasonName())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .status(season.getStatus() != null ? season.getStatus().getCode() : null)
                .plotId(season.getPlot() != null ? season.getPlot().getId() : null)
                .cropId(season.getCrop() != null ? season.getCrop().getId() : null)
                .varietyId(season.getVariety() != null ? season.getVariety().getId() : null)
                .plannedHarvestDate(season.getPlannedHarvestDate())
                .expectedYieldKg(season.getExpectedYieldKg())
                .actualYieldKg(season.getActualYieldKg())
                .build();
    }
}
