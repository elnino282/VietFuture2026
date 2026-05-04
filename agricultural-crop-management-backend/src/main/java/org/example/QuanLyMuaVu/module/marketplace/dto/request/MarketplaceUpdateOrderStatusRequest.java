package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;

public record MarketplaceUpdateOrderStatusRequest(
        @NotNull MarketplaceOrderStatus status) {
}
