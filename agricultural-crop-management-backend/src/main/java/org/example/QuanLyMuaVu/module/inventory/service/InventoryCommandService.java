package org.example.QuanLyMuaVu.module.inventory.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryCommandPort;
import org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InventoryCommandService implements InventoryCommandPort {

    ProductWarehouseService productWarehouseService;
    HarvestQueryPort harvestQueryPort;
    IdentityQueryPort identityQueryPort;

    @Override
    public ProductWarehouseLot receiveFromHarvest(Integer harvestId, Long actorUserId) {
        return receiveFromHarvest(harvestId, actorUserId, null);
    }

    @Override
    public ProductWarehouseLot receiveFromHarvest(Integer harvestId, Long actorUserId, ReceiveHarvestRequest request) {
        org.example.QuanLyMuaVu.module.season.entity.Harvest harvest = harvestQueryPort.findHarvestById(harvestId)
                .orElseThrow(() -> new AppException(ErrorCode.HARVEST_NOT_FOUND));
        org.example.QuanLyMuaVu.module.identity.entity.User actor = null;
        if (actorUserId != null) {
            actor = identityQueryPort.findUserById(actorUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }
        return productWarehouseService.receiveFromHarvest(harvest, actor, request);
    }
}
