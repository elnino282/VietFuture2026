package org.example.QuanLyMuaVu.module.cropcatalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.CropResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.service.CropService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalog endpoints for crops and varieties selection (used in dropdowns).
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class CatalogController {

    CropService cropService;

    @Operation(summary = "Get all crops", description = "Get list of all crops for dropdown selection")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/crops")
    public ApiResponse<List<CropResponse>> getAllCrops() {
        return ApiResponse.success(cropService.getAll());
    }

    @Operation(summary = "Get varieties by crop", description = "Get list of varieties for a specific crop")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Crop not found")
    })
    @GetMapping("/crops/{cropId}/varieties")
    public ApiResponse<List<VarietyResponse>> getVarietiesByCrop(@PathVariable Integer cropId) {
        return ApiResponse.success(cropService.getVarietiesByCropId(cropId));
    }
}


