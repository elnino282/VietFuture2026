package org.example.farm.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = \"season-service\", url = \"${app.season-service-url}\", fallback = SeasonProductionDiaryClientFallback.class)
public interface SeasonProductionDiaryClient {

    @GetMapping(\"/api/v1/internal/seasons/{seasonId}/production-diary\")
    List<ProductionDiaryEventDto> getProductionDiaryInternal(@PathVariable(\"seasonId\") Integer seasonId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ProductionDiaryEventDto {
        private java.time.LocalDate eventDate;
        private String eventType;
        private String title;
        private String description;
        private String sourceService;
        private Integer sourceId;
    }
}

