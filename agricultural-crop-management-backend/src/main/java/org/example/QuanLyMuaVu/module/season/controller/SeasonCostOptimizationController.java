package org.example.QuanLyMuaVu.module.season.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.ai.dto.request.SeasonCostOptimizationSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSuggestionResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.SeasonCostOptimizationSummaryResponse;
import org.example.QuanLyMuaVu.module.ai.service.SeasonCostOptimizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seasons/{seasonId}/cost-optimization")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class SeasonCostOptimizationController {

    SeasonCostOptimizationService seasonCostOptimizationService;

    @GetMapping("/summary")
    public ApiResponse<SeasonCostOptimizationSummaryResponse> getSummary(
            @PathVariable Integer seasonId,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {
        return ApiResponse.success(seasonCostOptimizationService.getSummary(seasonId, acceptLanguage));
    }

    @PostMapping("/ai-suggestion")
    public ApiResponse<SeasonCostOptimizationSuggestionResponse> generateSuggestion(
            @PathVariable Integer seasonId,
            @Valid @RequestBody(required = false) SeasonCostOptimizationSuggestionRequest request,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {
        return ApiResponse.success(seasonCostOptimizationService.generateSuggestion(seasonId, request, acceptLanguage));
    }
}
