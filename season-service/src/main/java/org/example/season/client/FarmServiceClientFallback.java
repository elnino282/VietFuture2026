package org.example.season.client;

import lombok.extern.slf4j.Slf4j;
import org.example.season.dto.PlotGeoJsonDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FarmServiceClientFallback implements FarmServiceClient {

    @Override
    public PlotGeoJsonDto getPlotGeoJson(Long plotId) {
        log.error("Fallback triggered: Failed to fetch geojson for plot {} from farm-service", plotId);
        return null;
    }

    @Override
    public Map<Long, PlotInternalDto> getBulkPlots(List<Long> plotIds) {
        log.error("Fallback triggered: Failed to fetch bulk plots from farm-service");
        return Collections.emptyMap();
    }

    @Override
    public FarmServiceClient.PlotInternalDto getPlot(Integer plotId) {
        log.error("Fallback triggered: Failed to fetch plot {} from farm-service", plotId);
        return null;
    }

    @Override
    public List<Integer> getAccessibleFarmIdsForUser(Long userId) {
        log.error("Fallback triggered: Failed to fetch farm IDs for user {} from farm-service", userId);
        return Collections.emptyList();
    }
}
