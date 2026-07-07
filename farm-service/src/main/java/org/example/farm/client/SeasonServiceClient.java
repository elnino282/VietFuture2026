package org.example.farm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "season-service", url = "${app.season-service-url:http://localhost:8085}", fallback = SeasonServiceClientFallback.class)
public interface SeasonServiceClient {

    @GetMapping("/api/v1/public/seasons/exists-active-by-plot/{plotId}")
    Boolean existsActiveSeasonsByPlot(@PathVariable("plotId") Integer plotId);

    @GetMapping("/api/v1/public/seasons/exists-active-tasks-by-plot/{plotId}")
    Boolean existsActiveTasksByPlot(@PathVariable("plotId") Integer plotId);

    @GetMapping("/api/v1/internal/seasons/{seasonId}/phi/active")
    java.util.List<PesticideRecordInternalDto> getActivePHIInternal(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/{seasonId}/logs/count")
    Long countFieldLogsByTypeInternal(@PathVariable("seasonId") Integer seasonId, @RequestParam("logType") String logType);

    @GetMapping("/api/v1/internal/plots/{plotId}/seasons")
    java.util.List<SeasonSummaryDto> getSeasonsByPlotId(@PathVariable("plotId") Integer plotId);

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class SeasonSummaryDto {
        private Integer id;
        private String seasonName;
        private Integer plotId;
        private Integer cropId;
        private String status;
        private java.time.LocalDate startDate;
        private java.time.LocalDate plannedHarvestDate;
        private java.time.LocalDate endDate;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class PesticideRecordInternalDto {
        private Integer id;
        private Integer seasonId;
        private String pesticideName;
        private String activeIngredient;
        private Integer phiDays;
        private java.time.LocalDate harvestAllowedDate;
        private java.time.LocalDate applicationDate;
    }
}
