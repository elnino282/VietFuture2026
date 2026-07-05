package org.example.marketplace.client.impl;

import org.example.marketplace.client.FarmClient;
import org.example.marketplace.dto.client.FarmDetailDto;
import org.example.marketplace.dto.client.FarmSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FarmClientFallback implements FarmClient {
    private static final Logger log = LoggerFactory.getLogger(FarmClientFallback.class);

    @Override
    public List<FarmSummaryDto> getFarmsByIds(List<Integer> farmIds) {
        log.error("Fallback triggered for getFarmsByIds with farmIds={}. Farm service might be down.", farmIds);
        return List.of();
    }

    @Override
    public FarmDetailDto getFarmDetail(Integer farmId) {
        log.error("Fallback triggered for getFarmDetail with farmId={}. Farm service might be down.", farmId);
        return null;
    }

    @Override
    public List<Integer> getFarmIdsByUserId(Long userId) {
        log.error("Fallback triggered for getFarmIdsByUserId with userId={}. Farm service might be down.", userId);
        return List.of();
    }
}
