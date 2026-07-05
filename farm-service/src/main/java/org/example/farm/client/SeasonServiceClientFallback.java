package org.example.farm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeasonServiceClientFallback implements SeasonServiceClient {

    @Override
    public Boolean existsActiveSeasonsByPlot(Integer plotId) {
        log.error("Fallback triggered: Failed to check active seasons for plot {} via season-service", plotId);
        return false;
    }

    @Override
    public Boolean existsActiveTasksByPlot(Integer plotId) {
        log.error("Fallback triggered: Failed to check active tasks for plot {} via season-service", plotId);
        return false;
    }
}
