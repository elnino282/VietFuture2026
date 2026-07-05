package org.example.incident.client;

import org.example.incident.service.ExternalServiceClient.SeasonInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.example.incident.client.fallback.SeasonClientFallback;
import java.util.List;

@FeignClient(name = "season-service", url = "${app.season-service-url:http://localhost:8085}", fallback = SeasonClientFallback.class)
public interface SeasonFeignClient {
    @GetMapping("/api/v1/internal/seasons/{seasonId}")
    SeasonInternalDto getSeason(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/owner/{ownerId}/ids")
    List<Integer> getSeasonIdsByOwnerId(@PathVariable("ownerId") Long ownerId);
}
