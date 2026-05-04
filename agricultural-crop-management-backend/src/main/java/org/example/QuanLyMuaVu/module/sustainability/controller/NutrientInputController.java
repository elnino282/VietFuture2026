package org.example.QuanLyMuaVu.module.sustainability.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateNutrientInputRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.NutrientInputResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.NutrientInputIngestionService;
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
public class NutrientInputController {

    NutrientInputIngestionService nutrientInputIngestionService;

    @PostMapping("/seasons/{seasonId}/nutrient-inputs")
    public ApiResponse<NutrientInputResponse> createNutrientInput(
            @PathVariable Integer seasonId,
            @Valid @RequestBody CreateNutrientInputRequest request
    ) {
        return ApiResponse.success(nutrientInputIngestionService.create(seasonId, request));
    }

    @GetMapping("/seasons/{seasonId}/nutrient-inputs")
    public ApiResponse<List<NutrientInputResponse>> listNutrientInputs(
            @PathVariable Integer seasonId,
            @RequestParam(required = false) Integer plotId,
            @RequestParam(required = false) NutrientInputSource source
    ) {
        return ApiResponse.success(nutrientInputIngestionService.list(seasonId, plotId, source));
    }
}
