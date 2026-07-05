package org.example.season.client;

import org.example.season.service.ExternalServiceClient.CropInternalDto;
import org.example.season.service.ExternalServiceClient.VarietyInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "crop-catalog-service", fallback = CropCatalogClientFallback.class)
public interface CropCatalogClient {
    
    @GetMapping("/api/v1/crops/{cropId}")
    CropDto getCropById(@PathVariable("cropId") Integer cropId);

    @GetMapping("/api/v1/internal/crops/{cropId}")
    CropInternalDto getInternalCrop(@PathVariable("cropId") Integer cropId);

    @GetMapping("/api/v1/internal/varieties/{varietyId}")
    VarietyInternalDto getVariety(@PathVariable("varietyId") Integer varietyId);

    // Inner DTO for convenience in this context
    class CropDto {
        private Integer id;
        private String cropName;
        private String category;
        private Integer postHarvestDelayDays;
        private Integer shelfLifeDays;

        public Integer getPostHarvestDelayDays() {
            return postHarvestDelayDays;
        }

        public void setPostHarvestDelayDays(Integer postHarvestDelayDays) {
            this.postHarvestDelayDays = postHarvestDelayDays;
        }
    }
}
