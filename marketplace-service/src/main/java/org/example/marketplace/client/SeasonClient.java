package org.example.marketplace.client;

import org.example.marketplace.dto.client.SeasonDetailDto;
import java.util.List;

public interface SeasonClient {
    List<SeasonDetailDto> getSeasonsByIds(List<Integer> seasonIds);
    SeasonDetailDto getSeasonDetail(Integer seasonId);
    List<Integer> getSeasonIdsByOwnerId(Long ownerId);
}
