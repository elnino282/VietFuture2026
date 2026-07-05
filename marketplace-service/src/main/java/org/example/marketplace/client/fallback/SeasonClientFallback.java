package org.example.marketplace.client.fallback;

import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.dto.client.SeasonDetailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class SeasonClientFallback implements SeasonClient {

    @Override
    public List<SeasonDetailDto> getSeasonsByIds(List<Integer> seasonIds) {
        log.error("Fallback triggered: Failed to fetch seasons by IDs");
        return Collections.emptyList();
    }

    @Override
    public SeasonDetailDto getSeasonDetail(Integer seasonId) {
        log.error("Fallback triggered: Failed to fetch season detail for seasonId={}", seasonId);
        return null;
    }

    @Override
    public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        log.error("Fallback triggered: Failed to fetch season IDs for ownerId={}", ownerId);
        return Collections.emptyList();
    }
}
