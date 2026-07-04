package org.example.inventory.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.inventory.dto.common.ApiResponse;
import org.example.inventory.dto.request.ReceiveToWarehouseRequest;
import org.example.inventory.dto.response.ProductWarehouseLotResponse;
import org.example.inventory.service.ProductWarehouseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-warehouse-lots")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class ProductWarehouseLotController {

    ProductWarehouseService productWarehouseService;

    /**
     * Nhập kho: Xác nhận nhập lô hàng vào kho, tính toán khối lượng tịnh từ độ ẩm.
     * Frontend gọi: POST /api/v1/product-warehouse-lots/{id}/receive-to-warehouse
     */
    @PostMapping("/{id}/receive-to-warehouse")
    public ApiResponse<ProductWarehouseLotResponse> receiveToWarehouse(
            @PathVariable Integer id,
            @Valid @RequestBody ReceiveToWarehouseRequest request) {
        return ApiResponse.success(productWarehouseService.receiveToWarehouse(id, request));
    }
}
