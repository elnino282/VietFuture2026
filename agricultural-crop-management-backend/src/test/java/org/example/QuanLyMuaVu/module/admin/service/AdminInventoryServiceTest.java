package org.example.QuanLyMuaVu.module.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryOptionsResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminInventoryRiskLotResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInventoryServiceTest {

    @Mock
    InventoryQueryPort inventoryQueryPort;
    @Mock
    FarmQueryPort farmQueryPort;

    @InjectMocks
    AdminInventoryService adminInventoryService;

    @Test
    @DisplayName("listRiskLots filters by farm, item, status, severity and flags abnormal movement")
    void listRiskLots_filtersAndFlagsAbnormalMovement() {
        Farm farmA = Farm.builder().id(1).name("Farm A").build();
        Warehouse warehouseA = Warehouse.builder().id(10).name("Input A").farm(farmA).build();
        SupplyItem item1 = SupplyItem.builder().id(101).name("Urea").unit("kg").build();

        SupplyLot lot1 = SupplyLot.builder()
                .id(1001)
                .supplyItem(item1)
                .batchCode("LOT-U-1")
                .expiryDate(LocalDate.now().plusDays(40))
                .status("IN_STOCK")
                .build();

        InventoryBalance balance1 = InventoryBalance.builder()
                .id(1L)
                .supplyLot(lot1)
                .warehouse(warehouseA)
                .quantity(BigDecimal.valueOf(120))
                .build();

        StockMovement adjust1 = StockMovement.builder()
                .id(11)
                .supplyLot(lot1)
                .warehouse(warehouseA)
                .movementType(StockMovementType.ADJUST)
                .quantity(BigDecimal.valueOf(10))
                .movementDate(LocalDateTime.now().minusDays(2))
                .build();
        StockMovement adjust2 = StockMovement.builder()
                .id(12)
                .supplyLot(lot1)
                .warehouse(warehouseA)
                .movementType(StockMovementType.ADJUST)
                .quantity(BigDecimal.valueOf(12))
                .movementDate(LocalDateTime.now().minusDays(1))
                .build();

        when(inventoryQueryPort.findAllInventoryBalancesWithDetails()).thenReturn(List.of(balance1));
        when(inventoryQueryPort.findStockMovementsBySupplyLotId(1001)).thenReturn(List.of(adjust1, adjust2));

        PageResponse<AdminInventoryRiskLotResponse> response = adminInventoryService.listRiskLots(
                1,
                101,
                "ABNORMAL_MOVEMENT",
                "HIGH",
                30,
                null,
                "EXPIRY_ASC",
                BigDecimal.valueOf(5),
                0,
                20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        AdminInventoryRiskLotResponse row = response.getItems().get(0);
        assertEquals(1001, row.getLotId());
        assertEquals("ABNORMAL_MOVEMENT", row.getStatus());
        assertEquals("HIGH", row.getSeverity());
        assertEquals(1, row.getFarmId());
        assertEquals(101, row.getItemId());
    }

    @Test
    @DisplayName("getOptions returns unique item options")
    void getOptions_returnsUniqueItems() {
        Farm farmA = Farm.builder().id(1).name("Farm A").build();
        SupplyItem item1 = SupplyItem.builder().id(101).name("Urea").unit("kg").build();
        SupplyItem item2 = SupplyItem.builder().id(102).name("NPK").unit("kg").build();

        SupplyLot lot1 = SupplyLot.builder().id(1).supplyItem(item1).status("IN_STOCK").build();
        SupplyLot lot2 = SupplyLot.builder().id(2).supplyItem(item1).status("IN_STOCK").build();
        SupplyLot lot3 = SupplyLot.builder().id(3).supplyItem(item2).status("IN_STOCK").build();

        when(farmQueryPort.findAllFarms()).thenReturn(List.of(farmA));
        when(inventoryQueryPort.findAllSupplyLots()).thenReturn(List.of(lot1, lot2, lot3));

        AdminInventoryOptionsResponse response = adminInventoryService.getOptions();

        assertNotNull(response);
        assertNotNull(response.getItems());
        assertEquals(2, response.getItems().size());
        assertFalse(response.getItems().stream().anyMatch(item -> item.getId() == null));
    }
}

