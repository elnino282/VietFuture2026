package org.example.season.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "sustainability-service", url = "${app.sustainability-service-url}", fallback = SustainabilityServiceClientFallback.class)
public interface SustainabilityServiceClient {

    @GetMapping("/api/v1/internal/seasons/{seasonId}/soil-tests")
    List<SoilTestInternalDto> getSoilTests(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/{seasonId}/irrigation-water-analyses")
    List<IrrigationWaterAnalysisInternalDto> getWaterAnalyses(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/{seasonId}/nutrient-inputs")
    List<NutrientInputEventInternalDto> getNutrientInputs(@PathVariable("seasonId") Integer seasonId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SoilTestInternalDto {
        private Integer id;
        private Integer seasonId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class IrrigationWaterAnalysisInternalDto {
        private Integer id;
        private Integer seasonId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NutrientInputEventInternalDto {
        private Integer id;
        private Integer seasonId;
        private java.time.LocalDate appliedDate;
        private String inputSource;
        private java.math.BigDecimal nKg;
    }
}

