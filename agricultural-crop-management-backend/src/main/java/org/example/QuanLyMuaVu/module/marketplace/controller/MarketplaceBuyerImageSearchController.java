package org.example.QuanLyMuaVu.module.marketplace.controller;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceImageSearchAnalysisResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceImageSearchResponse;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceImageSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// @RestController
// @RequestMapping("/api/v1/buyer/marketplace/image-search")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketplaceBuyerImageSearchController {

    MarketplaceImageSearchService marketplaceImageSearchService;

    @PostMapping("/analyze")
    public ApiResponse<MarketplaceImageSearchAnalysisResponse> analyze(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(marketplaceImageSearchService.analyze(file));
    }

    @PostMapping
    public ApiResponse<MarketplaceImageSearchResponse> search(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "traceable", required = false) Boolean traceable,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceImageSearchService.search(
                file,
                region,
                traceable,
                minPrice,
                maxPrice,
                sort,
                page,
                size));
    }
}
