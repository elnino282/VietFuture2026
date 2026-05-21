package org.example.QuanLyMuaVu.module.inventory.port;

import java.time.LocalDateTime;

public record InventoryLotMovementSummaryView(
        Integer lotId,
        long movementCount,
        LocalDateTime latestMovementDate) {
}
