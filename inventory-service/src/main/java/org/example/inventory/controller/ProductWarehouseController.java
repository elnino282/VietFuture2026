package org.example.inventory.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.ApiResponse;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.dto.request.AdjustProductWarehouseLotRequest;
import org.example.inventory.dto.request.CreateProductWarehouseLotRequest;
import org.example.inventory.dto.request.StockOutProductWarehouseLotRequest;
import org.example.inventory.dto.request.UpdateProductWarehouseLotRequest;
import org.example.inventory.dto.response.ProductWarehouseLotResponse;
import org.example.inventory.dto.response.ProductWarehouseOverviewResponse;
import org.example.inventory.dto.response.ProductWarehouseTraceabilityResponse;
import org.example.inventory.dto.response.ProductWarehouseTransactionResponse;
import org.example.inventory.service.ProductWarehousePublicService;
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
@RequestMapping("/api/v1/product-warehouses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class ProductWarehouseController {

    ProductWarehousePublicService productWarehousePublicService;

    @GetMapping("/overview")
    public ApiResponse<ProductWarehouseOverviewResponse> getOverview() {
        return ApiResponse.success(productWarehousePublicService.getOverview());
    }

    @GetMapping("/lots")
    public ApiResponse<PageResponse<ProductWarehouseLotResponse>> listLots(
            @RequestParam(value = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(value = "locationId", required = false) Integer locationId,
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "harvestedFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestedFrom,
            @RequestParam(value = "harvestedTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestedTo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(productWarehousePublicService.listLots(
                warehouseId,
                locationId,
                seasonId,
                farmId,
                plotId,
                harvestedFrom,
                harvestedTo,
                status,
                q,
                page,
                size));
    }

    @GetMapping("/lots/{id}")
    public ApiResponse<ProductWarehouseLotResponse> getLot(@PathVariable Integer id) {
        return ApiResponse.success(productWarehousePublicService.getLotDetail(id));
    }

    @PostMapping("/lots")
    public ApiResponse<ProductWarehouseLotResponse> createLot(
            @Valid @RequestBody CreateProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehousePublicService.createLot(request));
    }

    @PatchMapping("/lots/{id}")
    public ApiResponse<ProductWarehouseLotResponse> updateLot(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehousePublicService.updateLot(id, request));
    }

    @DeleteMapping("/lots/{id}")
    public ApiResponse<Void> archiveLot(@PathVariable Integer id) {
        productWarehousePublicService.archiveLot(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/lots/{id}/adjust")
    public ApiResponse<ProductWarehouseLotResponse> adjustLot(
            @PathVariable Integer id,
            @Valid @RequestBody AdjustProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehousePublicService.adjustLot(id, request));
    }

    @PostMapping("/lots/{id}/stock-out")
    public ApiResponse<ProductWarehouseLotResponse> stockOutLot(
            @PathVariable Integer id,
            @Valid @RequestBody StockOutProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehousePublicService.stockOutLot(id, request));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<ProductWarehouseTransactionResponse>> listTransactions(
            @RequestParam(value = "lotId", required = false) Integer lotId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(productWarehousePublicService.listTransactions(
                lotId,
                type,
                from,
                to,
                page,
                size));
    }

    @GetMapping("/lots/{id}/traceability")
    public ApiResponse<ProductWarehouseTraceabilityResponse> getTraceability(@PathVariable Integer id) {
        return ApiResponse.success(productWarehousePublicService.getTraceability(id));
    }
}
