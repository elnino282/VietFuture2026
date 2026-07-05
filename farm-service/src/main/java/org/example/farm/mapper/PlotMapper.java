package org.example.farm.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.farm.controller.InternalPlotController.PlotInternalDto;
import org.example.farm.controller.PlotGeoJsonController.PlotGeoJsonDto;
import org.example.farm.entity.Plot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class PlotMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "farmId", source = "farm.id")
    @Mapping(target = "farmName", source = "farm.name")
    @Mapping(target = "ownerUserId", source = "farm.userId")
    @Mapping(target = "farmActive", source = "farm.active")
    @Mapping(target = "plotArea", source = "area")
    public abstract PlotInternalDto toPlotInternalDto(Plot plot);

    @Mapping(target = "plotId", source = "id")
    @Mapping(target = "boundary", expression = "java(parseBoundaryGeoJson(plot.getBoundaryGeoJson()))")
    public abstract PlotGeoJsonDto toPlotGeoJsonDto(Plot plot);

    protected Map<String, Object> parseBoundaryGeoJson(String boundaryGeoJson) {
        if (boundaryGeoJson == null || boundaryGeoJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryGeoJson, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
