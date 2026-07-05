package org.example.incident.client.fallback;

import org.example.incident.client.FarmFeignClient;
import org.example.incident.service.ExternalServiceClient.FarmInternalDto;
import org.example.incident.service.ExternalServiceClient.PlotInternalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FarmClientFallback implements FarmFeignClient {
    private static final Logger log = LoggerFactory.getLogger(FarmClientFallback.class);

    @Override
    public PlotInternalDto getPlot(Integer plotId) {
        log.error("Fallback triggered for FarmFeignClient.getPlot with plotId={}. Farm service might be down.", plotId);
        return null;
    }

    @Override
    public FarmInternalDto getFarm(Integer farmId) {
        log.error("Fallback triggered for FarmFeignClient.getFarm with farmId={}. Farm service might be down.", farmId);
        return null;
    }
}
