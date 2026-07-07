package org.example.farm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "sustainability-service", url = "${app.sustainability-service-url:http://localhost:8089}", fallback = SustainabilityServiceClientFallback.class)
public interface SustainabilityServiceClient {

    @GetMapping("/api/v1/internal/seasons/{seasonId}/soil-tests")
    java.util.List<SoilTestInternalDto> getSoilTestsInternal(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/{seasonId}/irrigation-water-analyses")
    java.util.List<IrrigationWaterAnalysisInternalDto> getWaterAnalysesInternal(@PathVariable("seasonId") Integer seasonId);

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class SoilTestInternalDto {
        private Integer id;
        private Integer seasonId;
        private Integer plotId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class IrrigationWaterAnalysisInternalDto {
        private Integer id;
        private Integer seasonId;
        private Integer plotId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }
}
