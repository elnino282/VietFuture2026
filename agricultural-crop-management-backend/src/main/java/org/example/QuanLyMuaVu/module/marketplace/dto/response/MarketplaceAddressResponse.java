package org.example.QuanLyMuaVu.module.marketplace.dto.response;

public record MarketplaceAddressResponse(
        Long id,
        Long userId,
        String fullName,
        String phone,
        String province,
        String district,
        String ward,
        String street,
        String detail,
        String label,
        Boolean isDefault) {
}
