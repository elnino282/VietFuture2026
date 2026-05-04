package org.example.QuanLyMuaVu.module.cropcatalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.service.VarietyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/varieties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VarietyController {

    VarietyService varietyService;

    @Operation(summary = "Get crop variety", description = "Get a single crop variety by id")
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/{id}")
    public ApiResponse<VarietyResponse> get(@PathVariable Integer id) {
        return ApiResponse.success(varietyService.get(id));
    }

    @Operation(summary = "List varieties of crop", description = "List all varieties belonging to a crop")
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/by-crop/{cropId}")
    public ApiResponse<List<VarietyResponse>> listByCrop(@PathVariable Integer cropId) {
        return ApiResponse.success(varietyService.listByCrop(cropId));
    }
}


