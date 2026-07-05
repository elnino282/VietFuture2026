package org.example.incident.client.fallback;

import org.example.incident.client.SeasonFeignClient;
import org.example.incident.service.ExternalServiceClient.SeasonInternalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Collections;

@Component
public class SeasonClientFallback implements SeasonFeignClient {
    private static final Logger log = LoggerFactory.getLogger(SeasonClientFallback.class);

    @Override
    public SeasonInternalDto getSeason(Integer seasonId) {
        log.error("Fallback triggered for SeasonFeignClient.getSeason with seasonId={}. Season service might be down.", seasonId);
        return null;
    }

    @Override
    public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        log.error("Fallback triggered for SeasonFeignClient.getSeasonIdsByOwnerId with ownerId={}. Season service might be down.", ownerId);
        return Collections.emptyList();
    }
}
