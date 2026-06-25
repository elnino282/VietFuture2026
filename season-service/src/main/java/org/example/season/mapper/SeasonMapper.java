package org.example.season.mapper;

import lombok.RequiredArgsConstructor;
import org.example.season.dto.response.SeasonDetailResponse;
import org.example.season.dto.response.SeasonResponse;
import org.example.season.entity.Season;
import org.example.season.service.ExternalServiceClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeasonMapper {

    private final ExternalServiceClient externalServiceClient;

    public SeasonResponse toResponse(Season season) {
        if (season == null) {
            return null;
        }
        return SeasonResponse.builder()
                .id(season.getId())
                .seasonName(season.getSeasonName())
                .plotId(season.getPlotId())
                .cropId(season.getCropId())
                .varietyId(season.getVarietyId())
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

        String plotName = null;
        if (season.getPlotId() != null) {
            ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
            if (plot != null) {
                plotName = plot.getPlotName();
            }
        }

        String cropName = null;
        if (season.getCropId() != null) {
            ExternalServiceClient.CropInternalDto crop = externalServiceClient.getCrop(season.getCropId());
            if (crop != null) {
                cropName = crop.getCropName();
            }
        }

        String varietyName = null;
        if (season.getVarietyId() != null) {
            ExternalServiceClient.VarietyInternalDto variety = externalServiceClient.getVariety(season.getVarietyId());
            if (variety != null) {
                varietyName = variety.getName();
            }
        }

        return SeasonDetailResponse.builder()
                .id(season.getId())
                .seasonName(season.getSeasonName())
                .plotId(season.getPlotId())
                .cropId(season.getCropId())
                .plotName(plotName)
                .cropName(cropName)
                .varietyId(season.getVarietyId())
                .varietyName(varietyName)
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
