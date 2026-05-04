package org.example.QuanLyMuaVu.module.sustainability.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldRecommendationsResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FARMER')")
public class SustainabilityController {

    private final SustainabilityDashboardService sustainabilityDashboardService;

    @GetMapping("/dashboard/sustainability/overview")
    public ApiResponse<SustainabilityOverviewResponse> getOverview(
            @RequestParam(defaultValue = "field") String scope,
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) Integer farmId
    ) {
        log.debug("GET /dashboard/sustainability/overview scope={}, seasonId={}, fieldId={}, farmId={}",
                scope, seasonId, fieldId, farmId);
        return ApiResponse.success(sustainabilityDashboardService.getOverview(scope, seasonId, fieldId, farmId));
    }

    @GetMapping("/fields/map")
    public ApiResponse<FieldMapResponse> getFieldMap(
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String alertLevel
    ) {
        log.debug("GET /fields/map seasonId={}, farmId={}, crop={}, alertLevel={}",
                seasonId, farmId, crop, alertLevel);
        return ApiResponse.success(sustainabilityDashboardService.getFieldMap(seasonId, farmId, crop, alertLevel));
    }

    @GetMapping("/fields/{fieldId}/sustainability-metrics")
    public ApiResponse<SustainabilityOverviewResponse> getFieldMetrics(
            @PathVariable Integer fieldId,
            @RequestParam(required = false) Integer seasonId
    ) {
        log.debug("GET /fields/{}/sustainability-metrics seasonId={}", fieldId, seasonId);
        return ApiResponse.success(sustainabilityDashboardService.getFieldMetrics(fieldId, seasonId));
    }

    @GetMapping("/fields/{fieldId}/fdn-history")
    public ApiResponse<List<SustainabilityOverviewResponse.HistoryPoint>> getFieldHistory(
            @PathVariable Integer fieldId
    ) {
        log.debug("GET /fields/{}/fdn-history", fieldId);
        return ApiResponse.success(sustainabilityDashboardService.getFieldHistory(fieldId));
    }

    @GetMapping("/fields/{fieldId}/recommendations")
    public ApiResponse<FieldRecommendationsResponse> getFieldRecommendations(
            @PathVariable Integer fieldId,
            @RequestParam(required = false) Integer seasonId
    ) {
        log.debug("GET /fields/{}/recommendations seasonId={}", fieldId, seasonId);
        return ApiResponse.success(sustainabilityDashboardService.getFieldRecommendations(fieldId, seasonId));
    }
}
