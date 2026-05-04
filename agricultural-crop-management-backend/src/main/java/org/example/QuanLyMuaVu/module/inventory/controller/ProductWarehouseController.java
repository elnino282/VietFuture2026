package org.example.QuanLyMuaVu.module.inventory.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.request.AdjustProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.CreateProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.StockOutProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.UpdateProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseLotResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseOverviewResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseTraceabilityResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.ProductWarehouseTransactionResponse;
import org.example.QuanLyMuaVu.module.inventory.service.ProductWarehouseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    ProductWarehouseService productWarehouseService;

    @Operation(summary = "Product warehouse overview", description = "Get overview KPI for product warehouse")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/overview")
    public ApiResponse<ProductWarehouseOverviewResponse> getOverview() {
        return ApiResponse.success(productWarehouseService.getOverview());
    }

    @Operation(summary = "List product warehouse lots", description = "Get paginated product lots with filter and search")
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
        return ApiResponse.success(productWarehouseService.listLots(
                warehouseId, locationId, seasonId, farmId, plotId, harvestedFrom, harvestedTo, status, q, page, size));
    }

    @Operation(summary = "Get lot detail", description = "Get detail of a product warehouse lot")
    @GetMapping("/lots/{id}")
    public ApiResponse<ProductWarehouseLotResponse> getLot(@PathVariable Integer id) {
        return ApiResponse.success(productWarehouseService.getLotDetail(id));
    }

    @Operation(summary = "Create product lot", description = "Create a lot manually for product warehouse")
    @PostMapping("/lots")
    public ApiResponse<ProductWarehouseLotResponse> createLot(
            @Valid @RequestBody CreateProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehouseService.createLot(request));
    }

    @Operation(summary = "Update product lot", description = "Update metadata of a product lot")
    @PatchMapping("/lots/{id}")
    public ApiResponse<ProductWarehouseLotResponse> updateLot(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehouseService.updateLot(id, request));
    }

    @Operation(summary = "Archive product lot", description = "Archive a lot without deleting transaction history")
    @DeleteMapping("/lots/{id}")
    public ApiResponse<Void> archiveLot(@PathVariable Integer id) {
        productWarehouseService.archiveLot(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Adjust product lot on-hand", description = "Adjust on-hand quantity for a product lot")
    @PostMapping("/lots/{id}/adjust")
    public ApiResponse<ProductWarehouseLotResponse> adjustLot(
            @PathVariable Integer id,
            @Valid @RequestBody AdjustProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehouseService.adjustLot(id, request));
    }

    @Operation(summary = "Stock out product lot", description = "Stock out quantity from a product lot")
    @PostMapping("/lots/{id}/stock-out")
    public ApiResponse<ProductWarehouseLotResponse> stockOutLot(
            @PathVariable Integer id,
            @Valid @RequestBody StockOutProductWarehouseLotRequest request) {
        return ApiResponse.success(productWarehouseService.stockOutLot(id, request));
    }

    @Operation(summary = "List product warehouse transactions", description = "Get paginated transaction history")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<ProductWarehouseTransactionResponse>> listTransactions(
            @RequestParam(value = "lotId", required = false) Integer lotId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(productWarehouseService.listTransactions(lotId, type, from, to, page, size));
    }

    @Operation(summary = "Lot traceability", description = "Get traceability timeline of a product lot")
    @GetMapping("/lots/{id}/traceability")
    public ApiResponse<ProductWarehouseTraceabilityResponse> getTraceability(@PathVariable Integer id) {
        return ApiResponse.success(productWarehouseService.getTraceability(id));
    }
}
