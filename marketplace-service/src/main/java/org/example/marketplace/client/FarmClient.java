package org.example.marketplace.client;

import org.example.marketplace.dto.client.FarmDetailDto;
import org.example.marketplace.dto.client.FarmSummaryDto;
import java.util.List;

public interface FarmClient {
    List<FarmSummaryDto> getFarmsByIds(List<Integer> farmIds);
    FarmDetailDto getFarmDetail(Integer farmId);
    List<Integer> getFarmIdsByUserId(Long userId);
}
