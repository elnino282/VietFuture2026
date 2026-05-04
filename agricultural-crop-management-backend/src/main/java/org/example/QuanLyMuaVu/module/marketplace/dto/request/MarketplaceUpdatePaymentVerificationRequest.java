package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;

public record MarketplaceUpdatePaymentVerificationRequest(
        @NotNull MarketplacePaymentVerificationStatus verificationStatus,
        String verificationNote) {
}
