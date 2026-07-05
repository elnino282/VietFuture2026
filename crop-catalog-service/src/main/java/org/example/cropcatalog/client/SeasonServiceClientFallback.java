package org.example.cropcatalog.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeasonServiceClientFallback implements SeasonServiceClient {

    @Override
    public Boolean existsByVariety(Integer varietyId) {
        log.error("Fallback triggered: Failed to check if variety {} is referenced in seasons via season-service", varietyId);
        return false; // Safest fallback might be false or throwing exception, but interface returns Boolean.
    }
}
