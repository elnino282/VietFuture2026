package org.example.QuanLyMuaVu.module.inventory.port;

import java.math.BigDecimal;

public record InventoryLowStockView(
        Integer supplyLotId,
        String batchCode,
        String itemName,
        String warehouseName,
        String locationLabel,
        BigDecimal onHand,
        String unit) {
}
