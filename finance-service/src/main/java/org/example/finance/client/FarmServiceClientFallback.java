package org.example.finance.client;

import lombok.extern.slf4j.Slf4j;
import org.example.finance.service.ExternalServiceClient.PlotInternalDto;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FarmServiceClientFallback implements FarmServiceClient {

    @Override
    public PlotInternalDto getPlot(Integer plotId) {
        log.error("Fallback triggered: Failed to fetch plot {} from farm-service", plotId);
        return null;
    }
}
