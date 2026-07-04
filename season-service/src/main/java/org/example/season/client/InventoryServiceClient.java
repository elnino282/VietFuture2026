package org.example.season.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {

    @PostMapping("/api/v1/inventory/sync-harvest")
    void syncHarvestToInventory(@RequestBody HarvestSyncDto harvestSyncDto);

    // DTO for syncing
    class HarvestSyncDto {
        private Integer harvestId;
        private Integer seasonId;
        private java.math.BigDecimal netDryWeight;
        
        public HarvestSyncDto(Integer harvestId, Integer seasonId, java.math.BigDecimal netDryWeight) {
            this.harvestId = harvestId;
            this.seasonId = seasonId;
            this.netDryWeight = netDryWeight;
        }
        
        // getters and setters omitted for brevity
    }
}
