package org.example.marketplace.client.impl;

import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.dto.client.SeasonDetailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class SeasonClientImpl implements SeasonClient {

    private static final Logger log = LoggerFactory.getLogger(SeasonClientImpl.class);

    @Value("${external-services.season-service-url:http://season-service:8085}")
    private String seasonServiceUrl;

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(seasonServiceUrl)
                .build();
    }

    @Override
    public List<SeasonDetailDto> getSeasonsByIds(List<Integer> seasonIds) {
        if (seasonIds == null || seasonIds.isEmpty()) {
            return List.of();
        }
        try {
            return getWebClient().post()
                    .uri("/api/v1/internal/seasons/batch")
                    .bodyValue(seasonIds)
                    .retrieve()
                    .bodyToFlux(SeasonDetailDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch seasons by IDs: {}. Returning empty list.", e.getMessage());
            return List.of();
        }
    }

    @Override
    public SeasonDetailDto getSeasonDetail(Integer seasonId) {
        if (seasonId == null) {
            return null;
        }
        try {
            return getWebClient().get()
                    .uri("/api/v1/internal/seasons/{seasonId}", seasonId)
                    .retrieve()
                    .bodyToMono(SeasonDetailDto.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch season detail for seasonId={}: {}. Returning null.", seasonId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        try {
            return getWebClient().get()
                    .uri("/api/v1/internal/seasons/owner/{ownerId}/ids", ownerId)
                    .retrieve()
                    .bodyToFlux(Integer.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch season IDs for ownerId={}: {}. Returning empty list.", ownerId, e.getMessage());
            return List.of();
        }
    }
}
