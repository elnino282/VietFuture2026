package org.example.inventory.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.ApiResponse;
import org.example.inventory.dto.common.PageResponse;
import org.example.inventory.dto.request.CreateSupplierRequest;
import org.example.inventory.dto.request.CreateSupplyItemRequest;
import org.example.inventory.dto.request.StockInRequest;
import org.example.inventory.dto.request.UpdateSupplierRequest;
import org.example.inventory.dto.request.UpdateSupplyItemRequest;
import org.example.inventory.dto.response.StockInResponse;
import org.example.inventory.dto.response.SupplierResponse;
import org.example.inventory.dto.response.SupplyItemResponse;
import org.example.inventory.dto.response.SupplyLotResponse;
import org.example.inventory.service.SuppliesPublicService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supplies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class SuppliesController {

    SuppliesPublicService suppliesPublicService;

    @GetMapping("/suppliers")
    public ApiResponse<PageResponse<SupplierResponse>> getSuppliers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesPublicService.getSuppliers(q, pageable));
    }

    @GetMapping("/suppliers/{id}")
    public ApiResponse<SupplierResponse> getSupplier(@PathVariable Integer id) {
        return ApiResponse.success(suppliesPublicService.getSupplier(id));
    }

    @PostMapping("/suppliers")
    public ApiResponse<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ApiResponse.success(suppliesPublicService.createSupplier(request));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResponse<SupplierResponse> updateSupplier(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return ApiResponse.success(suppliesPublicService.updateSupplier(id, request));
    }

    @DeleteMapping("/suppliers/{id}")
    public ApiResponse<Void> deleteSupplier(@PathVariable Integer id) {
        suppliesPublicService.deleteSupplier(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<SupplyItemResponse>> getSupplyItems(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "restricted", required = false) Boolean restricted,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesPublicService.getSupplyItems(q, restricted, pageable));
    }

    @PostMapping("/items")
    public ApiResponse<SupplyItemResponse> createSupplyItem(
            @Valid @RequestBody CreateSupplyItemRequest request) {
        return ApiResponse.success(suppliesPublicService.createSupplyItem(request));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<SupplyItemResponse> updateSupplyItem(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSupplyItemRequest request) {
        return ApiResponse.success(suppliesPublicService.updateSupplyItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteSupplyItem(@PathVariable Integer id) {
        suppliesPublicService.deleteSupplyItem(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/lots")
    public ApiResponse<PageResponse<SupplyLotResponse>> getSupplyLots(
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "supplierId", required = false) Integer supplierId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesPublicService.getSupplyLots(
                itemId,
                supplierId,
                status,
                q,
                pageable));
    }

    @PostMapping("/stock-in")
    public ApiResponse<StockInResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        return ApiResponse.success(suppliesPublicService.stockIn(request));
    }
}
