package org.example.QuanLyMuaVu.module.inventory.port;

import java.math.BigDecimal;

public record HarvestStockContextView(
        long matchingLots,
        BigDecimal onHandQuantity,
        String unit,
        String warehouseName) {
}
