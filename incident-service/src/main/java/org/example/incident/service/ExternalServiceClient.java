package org.example.incident.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ExternalServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.identity-service-url:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${app.farm-service-url:http://localhost:8084}")
    private String farmServiceUrl;

    @Value("${app.season-service-url:http://localhost:8085}")
    private String seasonServiceUrl;

    public UserInternalDto getUser(Long userId) {
        try {
            String url = identityServiceUrl + "/api/v1/internal/users/" + userId;
            return restTemplate.getForObject(url, UserInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch user {} from identity-service", userId, e);
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

    public SeasonInternalDto getSeason(Integer seasonId) {
        try {
            String url = seasonServiceUrl + "/api/v1/internal/seasons/" + seasonId;
            return restTemplate.getForObject(url, SeasonInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch season {} from season-service", seasonId, e);
            return null;
        }
    }

    public java.util.List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        try {
            String url = seasonServiceUrl + "/api/v1/internal/seasons/owner/" + ownerId + "/ids";
            Integer[] response = restTemplate.getForObject(url, Integer[].class);
            return response != null ? java.util.Arrays.asList(response) : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch season IDs for owner {} from season-service", ownerId, e);
            return java.util.Collections.emptyList();
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
    public static class SeasonInternalDto {
        private Integer id;
        private String seasonName;
        private String status;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private java.time.LocalDate plannedHarvestDate;
        private Integer plotId;
    }

    public FarmInternalDto getFarm(Integer farmId) {
        try {
            String url = farmServiceUrl + "/api/v1/internal/farms/" + farmId;
            return restTemplate.getForObject(url, FarmInternalDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch farm {} from farm-service", farmId, e);
            return null;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmInternalDto {
        private Integer id;
        private String name;
    }
}
