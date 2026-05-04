package org.example.QuanLyMuaVu.module.marketplace.dto.response;

public record MarketplaceFarmSummaryResponse(
        Integer id,
        String name,
        String region,
        String address,
        String coverImageUrl,
        Long productCount) {
}
