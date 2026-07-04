package org.example.season.client;

import org.example.season.dto.PlotGeoJsonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "farm-service")
public interface FarmServiceClient {
    
    @GetMapping("/api/v1/plots/{plotId}/geojson")
    PlotGeoJsonDto getPlotGeoJson(@PathVariable("plotId") Long plotId);

    @PostMapping("/api/v1/internal/plots/bulk")
    Map<Long, PlotInternalDto> getBulkPlots(@RequestBody List<Long> plotIds);

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

