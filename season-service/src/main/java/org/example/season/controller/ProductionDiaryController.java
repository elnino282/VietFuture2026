package org.example.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.season.dto.response.ProductionDiaryEventDto;
import org.example.season.service.ProductionDiaryAggregationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductionDiaryController {

    private final ProductionDiaryAggregationService productionDiaryAggregationService;

    @Operation(summary = "Get Production Diary", description = "Get aggregated production diary events for a season")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @GetMapping("/farmer/seasons/{seasonId}/production-diary")
    public org.example.season.dto.common.ApiResponse<List<ProductionDiaryEventDto>> getProductionDiary(@PathVariable Integer seasonId) {
        List<ProductionDiaryEventDto> events = productionDiaryAggregationService.getProductionDiary(seasonId);
        return org.example.season.dto.common.ApiResponse.success(events);
    }

    // Internal endpoint for exporting dossier
    @GetMapping("/internal/seasons/{seasonId}/production-diary")
    public List<ProductionDiaryEventDto> getProductionDiaryInternal(@PathVariable Integer seasonId) {
        return productionDiaryAggregationService.getProductionDiary(seasonId);
    }
}

