package org.example.farm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.farm.dto.common.ApiResponse;
import org.example.farm.dto.response.ProvinceResponse;
import org.example.farm.dto.response.WardResponse;
import org.example.farm.service.AddressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Address", description = "Address lookup and import operations")
public class AddressController {

    AddressService addressService;

    @Operation(summary = "List all provinces", description = "Get list of all provinces with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceResponse>> listProvinces(
            @Parameter(description = "Optional keyword to search by province name") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Optional type filter: 'thanh-pho' (city) or 'tinh' (province)") @RequestParam(value = "type", required = false) String type) {
        return ApiResponse.success(addressService.getAllProvinces(keyword, type));
    }

    @Operation(summary = "Get province by ID", description = "Get a single province by its ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Province not found")
    })
    @GetMapping("/provinces/{id}")
    public ApiResponse<ProvinceResponse> getProvince(@PathVariable Integer id) {
        return ApiResponse.success(addressService.getProvinceById(id));
    }

    @Operation(summary = "List wards by province", description = "Get list of wards for a specific province")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Province not found")
    })
    @GetMapping("/provinces/{provinceId}/wards")
    public ApiResponse<List<WardResponse>> listWardsByProvince(
            @PathVariable Integer provinceId,
            @Parameter(description = "Optional keyword to search by ward name") @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(addressService.getWardsByProvinceId(provinceId, keyword));
    }

    @Operation(summary = "Get ward by ID", description = "Get a single ward by its ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ward not found")
    })
    @GetMapping("/wards/{id}")
    public ApiResponse<WardResponse> getWard(@PathVariable Integer id) {
        return ApiResponse.success(addressService.getWardById(id));
    }

    @Operation(summary = "Get address statistics", description = "Get count of countries, provinces, and wards")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping("/stats")
    public ApiResponse<AddressService.AddressStats> getStats() {
        return ApiResponse.success(addressService.getStats());
    }

}
