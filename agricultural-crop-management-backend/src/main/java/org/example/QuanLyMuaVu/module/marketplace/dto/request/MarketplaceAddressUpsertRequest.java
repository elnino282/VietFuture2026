package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MarketplaceAddressUpsertRequest(
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$", message = "Phone must be a valid Vietnamese mobile number") String phone,
        @NotBlank String province,
        @NotBlank String district,
        @NotBlank String ward,
        @NotBlank String street,
        String detail,
        String label,
        Boolean isDefault) {
}
