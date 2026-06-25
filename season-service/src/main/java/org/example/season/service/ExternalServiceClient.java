package org.example.season.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.season.dto.request.ReceiveHarvestRequest;
import org.example.season.dto.request.SyncLotRequest;
import org.example.season.dto.response.ProductWarehouseLotDto;
import org.example.season.dto.response.HarvestStockContextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.farm-service-url}")
    private String farmServiceUrl;

    @Value("${app.crop-catalog-service-url}")
    private String cropCatalogServiceUrl;

    @Value("${app.identity-service-url}")
    private String identityServiceUrl;

    @Value("${app.monolith-service-url}")
    private String monolithServiceUrl;

    @Value("${app.inventory-service-url}")
    private String inventoryServiceUrl;

    public Boolean existsExpenseBySeasonId(Integer seasonId) {
        try {
            String url = monolithServiceUrl + "/api/v1/public/expenses/exists-by-season/" + seasonId;
            return restTemplate.getForObject(url, Boolean.class);
        } catch (Exception e) {
            log.error("Failed to check expenses for season {} from monolith-service", seasonId, e);
            return false;
        }
    }

    public ValidationResultDto validateIncidentSeason(Integer incidentId, Integer seasonId) {
        try {
            String url = monolithServiceUrl + "/api/v1/public/lookup/incidents/" + incidentId + "/validate-season/" + seasonId;
            return restTemplate.getForObject(url, ValidationResultDto.class);
        } catch (Exception e) {
            log.error("Failed to validate incident {} for season {}", incidentId, seasonId, e);
            return ValidationResultDto.builder().valid(false).errorCode("INTERNAL_SERVER_ERROR").build();
        }
    }

    public ValidationResultDto validateExpenseSeason(Integer expenseId, Integer seasonId) {
        try {
            String url = monolithServiceUrl + "/api/v1/public/lookup/expenses/" + expenseId + "/validate-season/" + seasonId;
            return restTemplate.getForObject(url, ValidationResultDto.class);
        } catch (Exception e) {
            log.error("Failed to validate expense {} for season {}", expenseId, seasonId, e);
            return ValidationResultDto.builder().valid(false).errorCode("INTERNAL_SERVER_ERROR").build();
        }
    }

    public ValidationResultDto validateSupplyLot(Integer lotId, Integer itemId, String farmIdsCsv) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/supplies/validate-lot?lotId=" + lotId 
                    + (itemId != null ? "&itemId=" + itemId : "") + "&farmIds=" + farmIdsCsv;
            return restTemplate.getForObject(url, ValidationResultDto.class);
        } catch (Exception e) {
            log.error("Failed to validate supply lot {}", lotId, e);
            return ValidationResultDto.builder().valid(false).errorCode("INTERNAL_SERVER_ERROR").build();
        }
    }

    public ValidationResultDto validateSupplyItem(Integer itemId, String farmIdsCsv) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/supplies/validate-item?itemId=" + itemId 
                    + "&farmIds=" + farmIdsCsv;
            return restTemplate.getForObject(url, ValidationResultDto.class);
        } catch (Exception e) {
            log.error("Failed to validate supply item {}", itemId, e);
            return ValidationResultDto.builder().valid(false).errorCode("INTERNAL_SERVER_ERROR").build();
        }
    }

    public String getSupplyItemName(Integer itemId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/supplies/items/" + itemId + "/name";
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Failed to get supply item name {}", itemId, e);
            return null;
        }
    }

    public String getSupplyLotBatchCode(Integer lotId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/supplies/lots/" + lotId + "/batch-code";
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Failed to get supply lot batch code {}", lotId, e);
            return null;
        }
    }

    public PlotInternalDto getPlot(Integer plotId) {
        try {
            String url = farmServiceUrl + "/api/v1/internal/plots/" + plotId;
            return restTemplate.getForObject(url, PlotInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch plot {} from farm-service", plotId, e);
            return null;
        }
    }

    public CropInternalDto getCrop(Integer cropId) {
        try {
            String url = cropCatalogServiceUrl + "/api/v1/internal/crops/" + cropId;
            return restTemplate.getForObject(url, CropInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch crop {} from crop-catalog-service", cropId, e);
            return null;
        }
    }

    public VarietyInternalDto getVariety(Integer varietyId) {
        try {
            String url = cropCatalogServiceUrl + "/api/v1/internal/varieties/" + varietyId;
            return restTemplate.getForObject(url, VarietyInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch variety {} from crop-catalog-service", varietyId, e);
            return null;
        }
    }

    public UserInternalDto getUser(Long userId) {
        try {
            String url = identityServiceUrl + "/api/v1/internal/users/" + userId;
            return restTemplate.getForObject(url, UserInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch user {} from identity-service", userId, e);
            return null;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlotInternalDto {
        private Integer id;
        private String plotName;
        private Integer farmId;
        private String farmName;
        private Long ownerUserId;
        private Boolean farmActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropInternalDto {
        private Integer id;
        private String cropName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarietyInternalDto {
        private Integer id;
        private String name;
        private Integer cropId;
    }

    public void createNotification(Long userId, String title, String message, String link) {
        try {
            String url = monolithServiceUrl + "/api/v1/public/lookup/notifications";
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .link(link)
                    .build();
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to create notification for user {}", userId, e);
        }
    }

    public EmployeePageResponse searchEmployees(String keyword, int page, int size) {
        try {
            String url = identityServiceUrl + "/api/v1/internal/users/employees?page=" + page + "&size=" + size
                    + (keyword != null ? "&keyword=" + keyword : "");
            return restTemplate.getForObject(url, EmployeePageResponse.class);
        } catch (Exception e) {
            log.error("Failed to search employees", e);
            return EmployeePageResponse.builder().content(java.util.Collections.emptyList()).build();
        }
    }

    public Boolean validateEmployee(Long userId) {
        try {
            String url = identityServiceUrl + "/api/v1/internal/users/employees/" + userId + "/validate";
            return restTemplate.getForObject(url, Boolean.class);
        } catch (Exception e) {
            log.error("Failed to validate employee {}", userId, e);
            return false;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInternalDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResultDto {
        private boolean valid;
        private String errorCode;
        private String errorMessage;
        private Integer resolvedItemId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNotificationRequest {
        private Long userId;
        private String title;
        private String message;
        private String link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeePageResponse {
        private java.util.List<UserInternalDto> content;
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    public ProductWarehouseLotDto receiveFromHarvest(Integer harvestId, Long actorUserId, ReceiveHarvestRequest request) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/receive-harvest?harvestId=" + harvestId 
                    + "&actorUserId=" + actorUserId;
            return restTemplate.postForObject(url, request, ProductWarehouseLotDto.class);
        } catch (Exception e) {
            log.error("Failed to receive from harvest {}", harvestId, e);
            throw new RuntimeException("Inventory service connection failed", e);
        }
    }

    public List<ProductWarehouseLotDto> findLotsBySeasonIds(List<Integer> seasonIds) {
        try {
            String idsCsv = seasonIds.stream().map(Object::toString).collect(Collectors.joining(","));
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/lots/by-seasons?seasonIds=" + idsCsv;
            ProductWarehouseLotDto[] response = restTemplate.getForObject(url, ProductWarehouseLotDto[].class);
            return response != null ? Arrays.asList(response) : List.of();
        } catch (Exception e) {
            log.error("Failed to find lots by seasons", e);
            return List.of();
        }
    }

    public ProductWarehouseLotDto findLotByHarvestId(Integer harvestId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/lots/by-harvest/" + harvestId;
            return restTemplate.getForObject(url, ProductWarehouseLotDto.class);
        } catch (Exception e) {
            log.error("Failed to find lot by harvest id {}", harvestId, e);
            return null;
        }
    }

    public List<ProductWarehouseLotDto> findLotsByHarvestIds(List<Integer> harvestIds) {
        try {
            String idsCsv = harvestIds.stream().map(Object::toString).collect(Collectors.joining(","));
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/lots/by-harvests?harvestIds=" + idsCsv;
            ProductWarehouseLotDto[] response = restTemplate.getForObject(url, ProductWarehouseLotDto[].class);
            return response != null ? Arrays.asList(response) : List.of();
        } catch (Exception e) {
            log.error("Failed to find lots by harvest ids", e);
            return List.of();
        }
    }

    public ProductWarehouseLotDto syncLinkedLotFromHarvest(Integer lotId, SyncLotRequest request) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/lots/" + lotId + "/sync";
            return restTemplate.postForObject(url, request, ProductWarehouseLotDto.class);
        } catch (Exception e) {
            log.error("Failed to sync lot {}", lotId, e);
            throw new RuntimeException("Syncing lot failed", e);
        }
    }

    public Boolean existsProductWarehouseLotByHarvestId(Integer harvestId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/exists-by-harvest/" + harvestId;
            return restTemplate.getForObject(url, Boolean.class);
        } catch (Exception e) {
            log.error("Failed to check if lot exists for harvest {}", harvestId, e);
            return false;
        }
    }

    public HarvestStockContextDto findHarvestStockContext(
            Integer farmId,
            Integer warehouseId,
            String productName,
            String lotCode) {
        try {
            String url = inventoryServiceUrl + "/api/v1/public/lookup/inventory/stock-context?farmId=" + farmId
                    + "&warehouseId=" + warehouseId + "&productName=" + productName + "&lotCode=" + lotCode;
            return restTemplate.getForObject(url, HarvestStockContextDto.class);
        } catch (Exception e) {
            log.error("Failed to find harvest stock context", e);
            return null;
        }
    }

    public List<Integer> getAccessibleFarmIdsForUser(Long userId) {
        try {
            String url = farmServiceUrl + "/api/v1/internal/users/" + userId + "/farms/ids";
            Integer[] response = restTemplate.getForObject(url, Integer[].class);
            return response != null ? Arrays.asList(response) : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch farm IDs for user {}", userId, e);
            return List.of();
        }
    }
}


