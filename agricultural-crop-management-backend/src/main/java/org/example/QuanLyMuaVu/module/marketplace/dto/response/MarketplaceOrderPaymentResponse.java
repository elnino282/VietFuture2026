package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;

public record MarketplaceOrderPaymentResponse(
        MarketplacePaymentMethod method,
        MarketplacePaymentVerificationStatus verificationStatus,
        String proofFileName,
        String proofContentType,
        String proofStoragePath,
        LocalDateTime proofUploadedAt,
        LocalDateTime verifiedAt,
        Long verifiedBy,
        String verificationNote) {
}
