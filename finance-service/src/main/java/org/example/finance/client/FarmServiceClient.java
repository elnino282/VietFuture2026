package org.example.finance.client;

import org.example.finance.service.ExternalServiceClient.PlotInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "farm-service", url = "${app.farm-service-url:http://localhost:8084}", fallback = FarmServiceClientFallback.class)
public interface FarmServiceClient {

    @GetMapping("/api/v1/internal/plots/{plotId}")
    PlotInternalDto getPlot(@PathVariable("plotId") Integer plotId);
}
