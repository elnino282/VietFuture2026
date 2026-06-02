package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.util.List;

public record MarketplaceImageSearchAnalysisResponse(
        String detectedProduct,
        String category,
        List<String> keywordsVi,
        List<String> keywordsEn,
        List<String> keywords,
        List<String> visualAttributes,
        Double confidence,
        String confidenceLabel,
        Boolean agricultural,
        String message) {
}
