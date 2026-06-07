package org.example.QuanLyMuaVu.module.marketplace.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddressUpsertRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateOrderRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateReviewRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceMergeCartRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceAddressResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCartResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCreateOrderResultResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmSummaryResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderPreviewResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductSummaryResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceReviewResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceTraceabilityResponse;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceProductImageStorageService;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceProductImageStorageService.StoredProductImage;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketplaceController {

    MarketplaceService marketplaceService;
    MarketplaceProductImageStorageService productImageStorageService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<MarketplaceProductSummaryResponse>> listProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "traceable", required = false) Boolean traceable,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listProducts(category, q, region, traceable, minPrice, maxPrice, sort, farmId, page, size));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<MarketplaceProductDetailResponse> getProductBySlug(@PathVariable String slug) {
        return ApiResponse.success(marketplaceService.getProductBySlug(slug));
    }

    @GetMapping("/product-images/{fileName:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String fileName) {
        StoredProductImage image = productImageStorageService.loadProductImage(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.fileName() + "\"")
                .body(image.resource());
    }

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<PageResponse<MarketplaceReviewResponse>> listProductReviews(
            @PathVariable Long productId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listProductReviews(productId, page, size));
    }

    @GetMapping("/farms")
    public ApiResponse<PageResponse<MarketplaceFarmSummaryResponse>> listFarms(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listFarms(q, region, page, size));
    }

    @GetMapping("/farms/{farmId}")
    public ApiResponse<MarketplaceFarmDetailResponse> getFarmDetail(@PathVariable Integer farmId) {
        return ApiResponse.success(marketplaceService.getFarmDetail(farmId));
    }

    @GetMapping("/farms/{farmId}/reviews")
    public ApiResponse<PageResponse<MarketplaceReviewResponse>> listFarmReviews(
            @PathVariable Integer farmId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listFarmReviews(farmId, page, size));
    }

    @GetMapping("/products/{productId}/traceability")
    public ApiResponse<MarketplaceTraceabilityResponse> getProductTraceability(@PathVariable Long productId) {
        return ApiResponse.success(marketplaceService.getTraceability(productId));
    }

    /**
     * @deprecated Use GET /api/v1/marketplace/products/{productId}/traceability instead.
     * Kept temporarily for backward compatibility.
     */
    @Deprecated
    @GetMapping("/traceability/{productId}")
    public ApiResponse<MarketplaceTraceabilityResponse> getTraceabilityLegacy(@PathVariable Long productId) {
        return ApiResponse.success(marketplaceService.getTraceability(productId));
    }

    @GetMapping("/cart")
    public ApiResponse<MarketplaceCartResponse> getCart() {
        return ApiResponse.success(marketplaceService.getCart());
    }

    @PostMapping("/cart/items")
    public ApiResponse<MarketplaceCartResponse> addCartItem(@Valid @RequestBody MarketplaceAddCartItemRequest request) {
        return ApiResponse.success(marketplaceService.addCartItem(request));
    }

    @PutMapping("/cart/items/{productId}")
    public ApiResponse<MarketplaceCartResponse> updateCartItem(
            @PathVariable Long productId,
            @Valid @RequestBody MarketplaceUpdateCartItemRequest request) {
        return ApiResponse.success(marketplaceService.updateCartItem(productId, request));
    }

    @PatchMapping("/cart/items/{productId}")
    public ApiResponse<MarketplaceCartResponse> patchCartItem(
            @PathVariable Long productId,
            @Valid @RequestBody MarketplaceUpdateCartItemRequest request) {
        return ApiResponse.success(marketplaceService.updateCartItem(productId, request));
    }

    @DeleteMapping("/cart/items/{productId}")
    public ApiResponse<MarketplaceCartResponse> removeCartItem(@PathVariable Long productId) {
        return ApiResponse.success(marketplaceService.removeCartItem(productId));
    }

    @PostMapping("/cart/merge")
    public ApiResponse<MarketplaceCartResponse> mergeCart(@Valid @RequestBody MarketplaceMergeCartRequest request) {
        return ApiResponse.success(marketplaceService.mergeCart(request));
    }

    @DeleteMapping("/cart")
    public ApiResponse<MarketplaceCartResponse> clearCart() {
        return ApiResponse.success(marketplaceService.clearCart());
    }

    @PostMapping("/orders/preview")
    public ApiResponse<MarketplaceOrderPreviewResponse> previewOrder(
            @Valid @RequestBody MarketplaceCreateOrderRequest request) {
        return ApiResponse.success(marketplaceService.previewOrder(request));
    }

    @PostMapping("/orders")
    public ApiResponse<MarketplaceCreateOrderResultResponse> createOrder(
            @Valid @RequestBody MarketplaceCreateOrderRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader) {
        return ApiResponse.success(marketplaceService.createOrder(request, idempotencyKeyHeader));
    }

    @PostMapping("/orders/{orderId}/payment-proof")
    public ApiResponse<MarketplaceOrderResponse> uploadPaymentProof(
            @PathVariable Long orderId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(marketplaceService.uploadPaymentProof(orderId, file));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<MarketplaceOrderResponse>> listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listOrders(status, page, size));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<MarketplaceOrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.success(marketplaceService.getOrderDetail(orderId));
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ApiResponse<MarketplaceOrderResponse> cancelOrderPut(@PathVariable Long orderId) {
        return ApiResponse.success(marketplaceService.cancelOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<MarketplaceOrderResponse> cancelOrderPost(@PathVariable Long orderId) {
        return ApiResponse.success(marketplaceService.cancelOrder(orderId));
    }

    @GetMapping("/addresses")
    public ApiResponse<List<MarketplaceAddressResponse>> listAddresses() {
        return ApiResponse.success(marketplaceService.listAddresses());
    }

    @PostMapping("/addresses")
    public ApiResponse<MarketplaceAddressResponse> createAddress(
            @Valid @RequestBody MarketplaceAddressUpsertRequest request) {
        return ApiResponse.success(marketplaceService.createAddress(request));
    }

    @PatchMapping("/addresses/{addressId}")
    public ApiResponse<MarketplaceAddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody MarketplaceAddressUpsertRequest request) {
        return ApiResponse.success(marketplaceService.updateAddress(addressId, request));
    }

    @PatchMapping("/addresses/{addressId}/default")
    public ApiResponse<MarketplaceAddressResponse> setDefaultAddress(@PathVariable Long addressId) {
        return ApiResponse.success(marketplaceService.setDefaultAddress(addressId));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long addressId) {
        marketplaceService.deleteAddress(addressId);
        return ApiResponse.success(null);
    }

    /**
     * @deprecated Use POST /api/v1/buyer/orders/{orderId}/reviews instead.
     * Kept temporarily for backward compatibility.
     */
    @Deprecated
    @PostMapping("/reviews")
    public ApiResponse<MarketplaceReviewResponse> createReview(
            @Valid @RequestBody MarketplaceCreateReviewRequest request) {
        return ApiResponse.success(marketplaceService.createReviewLegacy(request));
    }
}
