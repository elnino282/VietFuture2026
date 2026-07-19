package org.example.farm.client;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeasonProductionDiaryClientFallback implements SeasonProductionDiaryClient {

    @Override
    public List<ProductionDiaryEventDto> getProductionDiaryInternal(Integer seasonId) {
        return new ArrayList<>();
    }
}

