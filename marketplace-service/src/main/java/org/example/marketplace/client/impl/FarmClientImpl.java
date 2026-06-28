package org.example.marketplace.client.impl;

import org.example.marketplace.client.FarmClient;
import org.example.marketplace.dto.client.FarmDetailDto;
import org.example.marketplace.dto.client.FarmSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class FarmClientImpl implements FarmClient {

    private static final Logger log = LoggerFactory.getLogger(FarmClientImpl.class);

    @Value("${external-services.farm-service-url:http://farm-service:8084}")
    private String farmServiceUrl;

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(farmServiceUrl)
                .build();
    }

    @Override
    public List<FarmSummaryDto> getFarmsByIds(List<Integer> farmIds) {
        if (farmIds == null || farmIds.isEmpty()) {
            return List.of();
        }
        try {
            return getWebClient().post()
                    .uri("/api/v1/internal/farms/batch")
                    .bodyValue(farmIds)
                    .retrieve()
                    .bodyToFlux(FarmSummaryDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch farms by IDs: {}. Returning empty list.", e.getMessage());
            return List.of();
        }
    }

    @Override
    public FarmDetailDto getFarmDetail(Integer farmId) {
        if (farmId == null) {
            return null;
        }
        try {
            return getWebClient().get()
                    .uri("/api/v1/internal/farms/{farmId}", farmId)
                    .retrieve()
                    .bodyToMono(FarmDetailDto.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch farm detail for farmId={}: {}. Returning null.", farmId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<Integer> getFarmIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            return getWebClient().get()
                    .uri("/api/v1/internal/users/{userId}/farms/ids", userId)
                    .retrieve()
                    .bodyToFlux(Integer.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch farm IDs for userId={}: {}. Returning empty list.", userId, e.getMessage());
            return List.of();
        }
    }
}
