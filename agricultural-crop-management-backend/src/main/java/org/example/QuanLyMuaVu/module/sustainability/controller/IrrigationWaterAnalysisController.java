package org.example.QuanLyMuaVu.module.sustainability.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateIrrigationWaterAnalysisRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.IrrigationWaterAnalysisResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.IrrigationWaterAnalysisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class IrrigationWaterAnalysisController {

    IrrigationWaterAnalysisService irrigationWaterAnalysisService;

    @PostMapping("/seasons/{seasonId}/irrigation-water-analyses")
    public ApiResponse<IrrigationWaterAnalysisResponse> create(
            @PathVariable Integer seasonId,
            @Valid @RequestBody CreateIrrigationWaterAnalysisRequest request
    ) {
        return ApiResponse.success(irrigationWaterAnalysisService.create(seasonId, request));
    }

    @GetMapping("/seasons/{seasonId}/irrigation-water-analyses")
    public ApiResponse<List<IrrigationWaterAnalysisResponse>> list(
            @PathVariable Integer seasonId,
            @RequestParam(required = false) Integer plotId
    ) {
        return ApiResponse.success(irrigationWaterAnalysisService.list(seasonId, plotId));
    }
}
