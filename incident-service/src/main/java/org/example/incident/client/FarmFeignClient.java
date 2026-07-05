package org.example.incident.client;

import org.example.incident.service.ExternalServiceClient.FarmInternalDto;
import org.example.incident.service.ExternalServiceClient.PlotInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.example.incident.client.fallback.FarmClientFallback;

@FeignClient(name = "farm-service", url = "${app.farm-service-url:http://localhost:8084}", fallback = FarmClientFallback.class)
public interface FarmFeignClient {
    @GetMapping("/api/v1/internal/plots/{plotId}")
    PlotInternalDto getPlot(@PathVariable("plotId") Integer plotId);

    @GetMapping("/api/v1/internal/farms/{farmId}")
    FarmInternalDto getFarm(@PathVariable("farmId") Integer farmId);
}
