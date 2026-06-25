package org.example.inventory.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.ApiResponse;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.dto.request.CreateWarehouseRequest;
import org.example.inventory.dto.request.RecordStockMovementRequest;
import org.example.inventory.dto.request.UpdateWarehouseRequest;
import org.example.inventory.dto.response.OnHandRowResponse;
import org.example.inventory.dto.response.StockLocationResponse;
import org.example.inventory.dto.response.StockMovementResponse;
import org.example.inventory.dto.response.WarehouseResponse;
import org.example.inventory.service.InventoryPublicService;
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

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class InventoryController {

    InventoryPublicService inventoryPublicService;

    @GetMapping("/warehouses/my")
    public ApiResponse<List<WarehouseResponse>> getMyWarehouses(
            @RequestParam(value = "type", required = false) String type) {
        return ApiResponse.success(inventoryPublicService.getMyWarehouses(type));
    }

    @GetMapping("/warehouses/{warehouseId}")
    public ApiResponse<WarehouseResponse> getWarehouseById(@PathVariable Integer warehouseId) {
        return ApiResponse.success(inventoryPublicService.getWarehouseById(warehouseId));
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.success(inventoryPublicService.createWarehouse(request));
    }

    @PatchMapping("/warehouses/{warehouseId}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable Integer warehouseId,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.success(inventoryPublicService.updateWarehouse(warehouseId, request));
    }

    @DeleteMapping("/warehouses/{warehouseId}")
    public ApiResponse<Boolean> deleteWarehouse(@PathVariable Integer warehouseId) {
        inventoryPublicService.deleteWarehouse(warehouseId);
        return ApiResponse.success(Boolean.TRUE);
    }

    @GetMapping("/locations")
    public ApiResponse<List<StockLocationResponse>> getLocationsByWarehouse(
            @RequestParam("warehouseId") Integer warehouseId) {
        return ApiResponse.success(inventoryPublicService.getLocationsByWarehouse(warehouseId));
    }

    @GetMapping("/on-hand")
    public ApiResponse<PageResponse<OnHandRowResponse>> getOnHand(
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "locationId", required = false) Integer locationId,
            @RequestParam(value = "lotId", required = false) Integer lotId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(inventoryPublicService.getOnHandList(
                warehouseId,
                locationId,
                lotId,
                q,
                pageable));
    }

    @GetMapping("/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> getMovements(
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(inventoryPublicService.getMovements(warehouseId, type, from, to, pageable));
    }

    @PostMapping("/movements")
    public ApiResponse<StockMovementResponse> recordMovement(
            @Valid @RequestBody RecordStockMovementRequest request) {
        return ApiResponse.success(inventoryPublicService.recordMovement(request));
    }

    @GetMapping("/lots/{lotId}/on-hand")
    public ApiResponse<BigDecimal> getOnHandQuantity(
            @PathVariable Integer lotId,
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "locationId", required = false) Integer locationId) {
        return ApiResponse.success(inventoryPublicService.getOnHandQuantity(lotId, warehouseId, locationId));
    }
}
