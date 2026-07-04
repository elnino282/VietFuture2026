package org.example.season.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "crop-catalog-service")
public interface CropCatalogClient {
    
    @GetMapping("/api/v1/crops/{cropId}")
    CropDto getCropById(@PathVariable("cropId") Integer cropId);

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
