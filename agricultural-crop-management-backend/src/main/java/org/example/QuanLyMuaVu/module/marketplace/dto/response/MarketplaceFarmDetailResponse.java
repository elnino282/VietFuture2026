package org.example.QuanLyMuaVu.module.marketplace.dto.response;

public record MarketplaceFarmDetailResponse(
        Integer id,
        String name,
        String region,
        String address,
        String coverImageUrl,
        Long productCount,
        String description,
        Long ownerUserId,
        String ownerDisplayName,
        String contactPhone) {
}
