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

    @Override
    public java.util.List<SeasonServiceClient.PesticideRecordInternalDto> getActivePHIInternal(Integer seasonId) {
        log.error("Fallback triggered: Failed to get active PHI for season {} via season-service", seasonId);
        return java.util.Collections.emptyList();
    }

    @Override
    public Long countFieldLogsByTypeInternal(Integer seasonId, String logType) {
        log.error("Fallback triggered: Failed to count field logs of type {} for season {} via season-service", logType, seasonId);
        return 0L;
    }

    @Override
    public java.util.List<SeasonServiceClient.SeasonSummaryDto> getSeasonsByPlotId(Integer plotId) {
        log.error("Fallback triggered: Failed to get seasons for plot {} via season-service", plotId);
        return java.util.Collections.emptyList();
    }
}
