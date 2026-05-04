package org.example.QuanLyMuaVu.module.farm.mapper;

import org.example.QuanLyMuaVu.Enums.PlotStatus;
import org.example.QuanLyMuaVu.module.farm.dto.request.PlotRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.PlotResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class PlotMapper {
    public void updateEntity(Plot plot, PlotRequest request) {
        if (plot == null || request == null)
            return;
        plot.setPlotName(request.getPlotName());
        plot.setArea(request.getArea());
        // SoilType is a String field
        if (request.getSoilType() != null) {
            plot.setSoilType(request.getSoilType());
        }
        if (request.getBoundaryGeoJson() != null) {
            plot.setBoundaryGeoJson(request.getBoundaryGeoJson());
        }
        // Status: PlotRequest has PlotStatus enum, Plot entity has String
        if (request.getStatus() != null) {
            plot.setStatus(request.getStatus().name());
        }
    }

    public PlotResponse toResponse(Plot plot) {
        if (plot == null)
            return null;

        // Convert String status back to PlotStatus enum
        PlotStatus plotStatus = null;
        if (plot.getStatus() != null) {
            try {
                plotStatus = PlotStatus.valueOf(plot.getStatus());
            } catch (IllegalArgumentException e) {
                // If status string doesn't match enum, leave as null
                plotStatus = null;
            }
        }

        return PlotResponse.builder()
                .id(plot.getId())
                .plotName(plot.getPlotName())
                .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                .area(plot.getArea())
                .soilType(plot.getSoilType())
                .boundaryGeoJson(plot.getBoundaryGeoJson())
                .status(plotStatus)
                .build();
    }
}
