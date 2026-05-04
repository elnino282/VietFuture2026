package org.example.QuanLyMuaVu.module.inventory.port;

import java.util.Optional;

public interface ProductWarehouseTraceabilityReadPort {

    Optional<ProductWarehouseTraceabilitySummaryView> findTraceabilitySummaryByLotId(Integer lotId);
}

