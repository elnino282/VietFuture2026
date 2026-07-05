package org.example.marketplace.client;

import org.example.marketplace.dto.client.FarmDetailDto;
import org.example.marketplace.dto.client.FarmSummaryDto;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@FeignClient(name = "farm-service", url = "${external-services.farm-service-url:http://farm-service:8084}", fallbackFactory = org.example.marketplace.client.impl.FarmClientFallbackFactory.class)
public interface FarmClient {
    @PostMapping("/api/v1/internal/farms/batch")
    List<FarmSummaryDto> getFarmsByIds(@RequestBody List<Integer> farmIds);
    
    @GetMapping("/api/v1/internal/farms/{farmId}")
    FarmDetailDto getFarmDetail(@PathVariable("farmId") Integer farmId);
    
    @GetMapping("/api/v1/internal/users/{userId}/farms/ids")
    List<Integer> getFarmIdsByUserId(@PathVariable("userId") Long userId);
}
