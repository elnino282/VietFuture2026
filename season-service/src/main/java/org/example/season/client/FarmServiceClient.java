package org.example.season.client;

import org.example.season.dto.PlotGeoJsonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "farm-service", url = "${app.farm-service-url}", fallback = FarmServiceClientFallback.class)
public interface FarmServiceClient {
    
    @GetMapping("/api/v1/plots/{plotId}/geojson")
    PlotGeoJsonDto getPlotGeoJson(@PathVariable("plotId") Long plotId);

    @PostMapping("/api/v1/internal/plots/bulk")
    Map<Long, PlotInternalDto> getBulkPlots(@RequestBody List<Long> plotIds);

    @GetMapping("/api/v1/internal/plots/{plotId}")
    PlotInternalDto getPlot(@PathVariable("plotId") Integer plotId);

    @GetMapping("/api/v1/internal/users/{userId}/farms/ids")
    List<Integer> getAccessibleFarmIdsForUser(@PathVariable("userId") Long userId);

    @lombok.Data
    class PlotInternalDto {
        private Integer id;
        private String plotName;
        private java.math.BigDecimal plotArea;
        private Integer farmId;
        private String farmName;
        private Long ownerUserId;
        private Boolean farmActive;
    }
}

