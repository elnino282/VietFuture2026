package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;

public record MarketplaceUpdateProductStatusRequest(
        @NotNull MarketplaceProductStatus status) {
}
