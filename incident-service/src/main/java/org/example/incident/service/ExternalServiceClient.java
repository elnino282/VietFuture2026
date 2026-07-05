package org.example.incident.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.example.incident.client.FarmFeignClient;
import org.example.incident.client.IdentityFeignClient;
import org.example.incident.client.SeasonFeignClient;

@Service
@Slf4j
@AllArgsConstructor
public class ExternalServiceClient {

    private final FarmFeignClient farmFeignClient;
    private final IdentityFeignClient identityFeignClient;
    private final SeasonFeignClient seasonFeignClient;

    public UserInternalDto getUser(Long userId) {
        return identityFeignClient.getUser(userId);
    }

    public PlotInternalDto getPlot(Integer plotId) {
        return farmFeignClient.getPlot(plotId);
    }

    public SeasonInternalDto getSeason(Integer seasonId) {
        return seasonFeignClient.getSeason(seasonId);
    }

    public java.util.List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        return seasonFeignClient.getSeasonIdsByOwnerId(ownerId);
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
        return farmFeignClient.getFarm(farmId);
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
