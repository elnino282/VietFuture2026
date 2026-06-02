package org.example.QuanLyMuaVu.module.marketplace.dto.response;

public record MarketplaceProductImageUploadResponse(
        String url,
        String fileName,
        String contentType,
        long size) {
}
