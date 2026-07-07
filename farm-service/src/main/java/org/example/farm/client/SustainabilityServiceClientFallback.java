package org.example.farm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class SustainabilityServiceClientFallback implements SustainabilityServiceClient {

    @Override
    public List<SoilTestInternalDto> getSoilTestsInternal(Integer seasonId) {
        log.error("Fallback triggered: Failed to get soil tests for season {} via sustainability-service", seasonId);
        return Collections.emptyList();
    }

    @Override
    public List<IrrigationWaterAnalysisInternalDto> getWaterAnalysesInternal(Integer seasonId) {
        log.error("Fallback triggered: Failed to get water analyses for season {} via sustainability-service", seasonId);
        return Collections.emptyList();
    }
}
