package org.example.QuanLyMuaVu.module.inventory.port;

import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;

public interface InventoryCommandPort {

    ProductWarehouseLot receiveFromHarvest(Integer harvestId, Long actorUserId);

    ProductWarehouseLot receiveFromHarvest(Integer harvestId, Long actorUserId, ReceiveHarvestRequest request);
}
