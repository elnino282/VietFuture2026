package org.example.finance.client;

import lombok.extern.slf4j.Slf4j;
import org.example.finance.service.ExternalServiceClient.SeasonInternalDto;
import org.example.finance.service.ExternalServiceClient.TaskInternalDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class SeasonServiceClientFallback implements SeasonServiceClient {

    @Override
    public SeasonInternalDto getSeason(Integer seasonId) {
        log.error("Fallback triggered: Failed to fetch season {} from season-service", seasonId);
        return null;
    }

    @Override
    public TaskInternalDto getTask(Integer taskId) {
        log.error("Fallback triggered: Failed to fetch task {} from season-service", taskId);
        return null;
    }

    @Override
    public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        log.error("Fallback triggered: Failed to fetch season IDs for owner {} from season-service", ownerId);
        return Collections.emptyList();
    }
}
