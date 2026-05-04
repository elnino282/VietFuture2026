package org.example.QuanLyMuaVu.module.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.request.CreateWarehouseRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.RecordStockMovementRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateWarehouseRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.OnHandRowResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockLocationResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockMovementResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.WarehouseResponse;
import org.example.QuanLyMuaVu.module.inventory.service.InventoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for Inventory Management (Farmer Portal).
 * Manages warehouses, stock locations, on-hand quantities, and stock movements.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    InventoryService inventoryService;

    // ===================================
    // WAREHOUSES
    // ===================================

    @Operation(summary = "Get my warehouses", description = "Get all warehouses accessible to the current farmer (through farm ownership)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/warehouses/my")
    public ApiResponse<List<WarehouseResponse>> getMyWarehouses(
            @RequestParam(value = "type", required = false) String type) {
        return ApiResponse.success(inventoryService.getMyWarehouses(type));
    }

    @Operation(summary = "Get warehouse detail", description = "Get warehouse detail by ID")
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/warehouses/{warehouseId}")
    public ApiResponse<WarehouseResponse> getWarehouseById(@PathVariable Integer warehouseId) {
        return ApiResponse.success(inventoryService.getWarehouseById(warehouseId));
    }

    @Operation(summary = "Create warehouse", description = "Create a new warehouse for current farmer")
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/warehouses")
    public ApiResponse<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.success(inventoryService.createWarehouse(request));
    }

    @Operation(summary = "Update warehouse", description = "Update warehouse metadata")
    @PreAuthorize("hasRole('FARMER')")
    @PatchMapping("/warehouses/{warehouseId}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable Integer warehouseId,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.success(inventoryService.updateWarehouse(warehouseId, request));
    }

    @Operation(summary = "Delete warehouse", description = "Delete a warehouse when it has no dependent records")
    @PreAuthorize("hasRole('FARMER')")
    @DeleteMapping("/warehouses/{warehouseId}")
    public ApiResponse<Boolean> deleteWarehouse(@PathVariable Integer warehouseId) {
        inventoryService.deleteWarehouse(warehouseId);
        return ApiResponse.success(Boolean.TRUE);
    }

    // ===================================
    // LOCATIONS
    // ===================================

    @Operation(summary = "Get locations by warehouse", description = "Get all stock locations for a specific warehouse")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/locations")
    public ApiResponse<List<StockLocationResponse>> getLocationsByWarehouse(
            @RequestParam("warehouseId") Integer warehouseId) {
        return ApiResponse.success(inventoryService.getLocationsByWarehouse(warehouseId));
    }

    // ===================================
    // ON-HAND VIEW
    // ===================================

    @Operation(summary = "Get on-hand inventory", description = "Get paginated on-hand inventory rows for a warehouse")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/on-hand")
    public ApiResponse<PageResponse<OnHandRowResponse>> getOnHand(
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "locationId", required = false) Integer locationId,
            @RequestParam(value = "lotId", required = false) Integer lotId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(inventoryService.getOnHandList(warehouseId, locationId, lotId, q, pageable));
    }

    // ===================================
    // MOVEMENTS (HISTORY)
    // ===================================

    @Operation(summary = "Get stock movements", description = "Get paginated stock movement history for a warehouse")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> getMovements(
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(inventoryService.getMovements(warehouseId, type, from, to, pageable));
    }

    // ===================================
    // RECORD MOVEMENT (IN/OUT/ADJUST)
    // ===================================

    @Operation(summary = "Record stock movement", description = "Record inbound, outbound, or adjustment movement for a supply lot")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request (validation failed)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/movements")
    public ApiResponse<StockMovementResponse> recordMovement(
            @Valid @RequestBody RecordStockMovementRequest request) {
        return ApiResponse.success(inventoryService.recordMovement(request));
    }

    // ===================================
    // GET ON-HAND QUANTITY (Simple)
    // ===================================

    @Operation(summary = "Get on-hand quantity", description = "Get current on-hand quantity for a supply lot at a warehouse/location")
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/lots/{lotId}/on-hand")
    public ApiResponse<BigDecimal> getOnHandQuantity(
            @PathVariable Integer lotId,
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "locationId", required = false) Integer locationId) {
        return ApiResponse.success(inventoryService.getOnHandQuantity(lotId, warehouseId, locationId));
    }
}
