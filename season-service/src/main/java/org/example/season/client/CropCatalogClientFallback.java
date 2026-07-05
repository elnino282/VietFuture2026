package org.example.season.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CropCatalogClientFallback implements CropCatalogClient {

    @Override
    public CropDto getCropById(Integer cropId) {
        log.error("Fallback triggered: Failed to fetch crop {} from crop-catalog-service", cropId);
        return null;
    }

    @Override
    public org.example.season.service.ExternalServiceClient.CropInternalDto getInternalCrop(Integer cropId) {
        log.error("Fallback triggered: Failed to fetch internal crop {} from crop-catalog-service", cropId);
        return null;
    }

    @Override
    public org.example.season.service.ExternalServiceClient.VarietyInternalDto getVariety(Integer varietyId) {
        log.error("Fallback triggered: Failed to fetch variety {} from crop-catalog-service", varietyId);
        return null;
    }
}
