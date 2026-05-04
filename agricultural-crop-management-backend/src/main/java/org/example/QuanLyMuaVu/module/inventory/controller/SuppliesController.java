package org.example.QuanLyMuaVu.module.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.request.StockInRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockInResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplierResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyItemResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyLotResponse;
import org.example.QuanLyMuaVu.module.inventory.service.SuppliesService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for Suppliers & Supplies management (Farmer Portal).
 * Provides catalog views and Stock IN functionality.
 */
@RestController
@RequestMapping("/api/v1/supplies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuppliesController {

    SuppliesService suppliesService;

    // ===================================
    // CATALOG: SUPPLIERS
    // ===================================

    @Operation(summary = "List suppliers", description = "Get paginated list of suppliers with optional search")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/suppliers")
    public ApiResponse<PageResponse<SupplierResponse>> getSuppliers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesService.getSuppliers(q, pageable));
    }

    // ===================================
    // CATALOG: SUPPLY ITEMS
    // ===================================

    @Operation(summary = "List supply items", description = "Get paginated list of supply items with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/items")
    public ApiResponse<PageResponse<SupplyItemResponse>> getSupplyItems(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "restricted", required = false) Boolean restricted,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesService.getSupplyItems(q, restricted, pageable));
    }

    // ===================================
    // CATALOG: SUPPLY LOTS
    // ===================================

    @Operation(summary = "List supply lots", description = "Get paginated list of supply lots with optional filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/lots")
    public ApiResponse<PageResponse<SupplyLotResponse>> getSupplyLots(
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "supplierId", required = false) Integer supplierId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesService.getSupplyLots(itemId, supplierId, status, q, pageable));
    }

    // ===================================
    // STOCK IN
    // ===================================

    @Operation(summary = "Record Stock IN", description = "Create a new supply lot and record inbound stock movement")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request (validation failed or restricted confirmation required)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden (warehouse access denied)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier, item, warehouse, or location not found")
    })
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/stock-in")
    public ApiResponse<StockInResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        return ApiResponse.success(suppliesService.stockIn(request));
    }
}
