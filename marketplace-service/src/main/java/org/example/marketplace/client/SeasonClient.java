package org.example.marketplace.client;

import org.example.marketplace.dto.client.SeasonDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "season-service", url = "${external-services.season-service-url:http://season-service:8085}", fallbackFactory = org.example.marketplace.client.fallback.SeasonClientFallbackFactory.class)
public interface SeasonClient {

    @PostMapping("/api/v1/internal/seasons/batch")
    List<SeasonDetailDto> getSeasonsByIds(@RequestBody List<Integer> seasonIds);

    @GetMapping("/api/v1/internal/seasons/{seasonId}")
    SeasonDetailDto getSeasonDetail(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/seasons/owner/{ownerId}/ids")
    List<Integer> getSeasonIdsByOwnerId(@PathVariable("ownerId") Long ownerId);
}
