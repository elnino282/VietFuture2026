package org.example.farm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "season-service", url = "${app.season-service-url:http://localhost:8085}", fallback = SeasonServiceClientFallback.class)
public interface SeasonServiceClient {

    @GetMapping("/api/v1/public/seasons/exists-active-by-plot/{plotId}")
    Boolean existsActiveSeasonsByPlot(@PathVariable("plotId") Integer plotId);

    @GetMapping("/api/v1/public/seasons/exists-active-tasks-by-plot/{plotId}")
    Boolean existsActiveTasksByPlot(@PathVariable("plotId") Integer plotId);
}
