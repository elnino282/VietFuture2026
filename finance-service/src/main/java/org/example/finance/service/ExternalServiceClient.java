package org.example.finance.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finance.client.FarmServiceClient;
import org.example.finance.client.IdentityServiceClient;
import org.example.finance.client.SeasonServiceClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalServiceClient {

    private final IdentityServiceClient identityServiceClient;
    private final FarmServiceClient farmServiceClient;
    private final SeasonServiceClient seasonServiceClient;

    public UserInternalDto getUser(Long userId) {
        return identityServiceClient.getUser(userId);
    }

    public PlotInternalDto getPlot(Integer plotId) {
        return farmServiceClient.getPlot(plotId);
    }

    public SeasonInternalDto getSeason(Integer seasonId) {
        return seasonServiceClient.getSeason(seasonId);
    }

    public TaskInternalDto getTask(Integer taskId) {
        return seasonServiceClient.getTask(taskId);
    }

    public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
        return seasonServiceClient.getSeasonIdsByOwnerId(ownerId);
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
        private Integer plotId;
        private Integer cropId;
        private Integer varietyId;
        private String status;
        private java.time.LocalDate startDate;
        private java.time.LocalDate plannedHarvestDate;
        private java.time.LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskInternalDto {
        private Integer id;
        private String title;
        private Integer seasonId;
        private Long userId;
        private String status;
    }
}

