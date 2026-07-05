package org.example.finance.client;

import org.example.finance.service.ExternalServiceClient.SeasonInternalDto;
import org.example.finance.service.ExternalServiceClient.TaskInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "season-service", url = "${app.season-service-url:http://localhost:8085}", fallback = SeasonServiceClientFallback.class)
public interface SeasonServiceClient {

    @GetMapping("/api/v1/internal/seasons/{seasonId}")
    SeasonInternalDto getSeason(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/internal/tasks/{taskId}")
    TaskInternalDto getTask(@PathVariable("taskId") Integer taskId);

    @GetMapping("/api/v1/internal/seasons/owner/{ownerId}/ids")
    List<Integer> getSeasonIdsByOwnerId(@PathVariable("ownerId") Long ownerId);
}
