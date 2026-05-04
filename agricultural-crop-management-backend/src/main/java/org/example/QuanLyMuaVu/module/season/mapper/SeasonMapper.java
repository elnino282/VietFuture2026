package org.example.QuanLyMuaVu.module.season.mapper;

import org.example.QuanLyMuaVu.module.season.dto.response.SeasonDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.springframework.stereotype.Component;

@Component
public class SeasonMapper {

    public SeasonResponse toResponse(Season season) {
        if (season == null) {
            return null;
        }
        return SeasonResponse.builder()
                .id(season.getId())
                .seasonName(season.getSeasonName())
                .plotId(season.getPlot() != null ? season.getPlot().getId() : null)
                .cropId(season.getCrop() != null ? season.getCrop().getId() : null)
                .varietyId(season.getVariety() != null ? season.getVariety().getId() : null)
                .startDate(season.getStartDate())
                .plannedHarvestDate(season.getPlannedHarvestDate())
                .endDate(season.getEndDate())
                .status(season.getStatus() != null ? season.getStatus().getCode() : null)
                .expectedYieldKg(season.getExpectedYieldKg())
                .actualYieldKg(season.getActualYieldKg())
                .build();
    }

    public SeasonDetailResponse toDetailResponse(Season season) {
        if (season == null) {
            return null;
        }
        return SeasonDetailResponse.builder()
                .id(season.getId())
                .seasonName(season.getSeasonName())
                .plotId(season.getPlot() != null ? season.getPlot().getId() : null)
                .cropId(season.getCrop() != null ? season.getCrop().getId() : null)
                .plotName(season.getPlot() != null ? season.getPlot().getPlotName() : null)
                .cropName(season.getCrop() != null ? season.getCrop().getCropName() : null)
                .varietyId(season.getVariety() != null ? season.getVariety().getId() : null)
                .varietyName(season.getVariety() != null ? season.getVariety().getName() : null)
                .startDate(season.getStartDate())
                .plannedHarvestDate(season.getPlannedHarvestDate())
                .endDate(season.getEndDate())
                .status(season.getStatus() != null ? season.getStatus().getCode() : null)
                .initialPlantCount(season.getInitialPlantCount())
                .currentPlantCount(season.getCurrentPlantCount())
                .expectedYieldKg(season.getExpectedYieldKg())
                .actualYieldKg(season.getActualYieldKg())
                .notes(season.getNotes())
                .build();
    }
}
