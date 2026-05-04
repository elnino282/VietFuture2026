package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryCommandPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.season.mapper.HarvestMapper;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.service.SeasonHarvestService;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonHarvestServiceProductWarehouseTest {

    @Mock
    HarvestRepository harvestRepository;
    @Mock
    SeasonRepository seasonRepository;
    @Mock
    HarvestMapper harvestMapper;
    @Mock
    FarmAccessPort farmAccessService;
    @Mock
    InventoryCommandPort inventoryCommandPort;
    @Mock
    InventoryQueryPort inventoryQueryPort;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    SeasonHarvestService seasonHarvestService;

    @Test
    @DisplayName("createHarvest auto-receives to product warehouse")
    void createHarvest_autoReceivesProductWarehouse() {
        Farm farm = Farm.builder().id(1).build();
        Plot plot = Plot.builder().id(2).farm(farm).build();
        Season season = Season.builder()
                .id(10)
                .plot(plot)
                .status(SeasonStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 30))
                .build();
        User actor = User.builder().id(2L).username("farmer").build();

        CreateHarvestDetailRequest request = CreateHarvestDetailRequest.builder()
                .harvestDate(LocalDate.of(2026, 3, 10))
                .quantity(BigDecimal.valueOf(30))
                .unit(BigDecimal.valueOf(1))
                .warehouseId(7)
                .productName("Rice")
                .lotCode("LOT-20260310-01")
                .grade("A")
                .note("batch 1")
                .build();

        Harvest saved = Harvest.builder()
                .id(100)
                .season(season)
                .harvestDate(request.getHarvestDate())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .grade(request.getGrade())
                .note(request.getNote())
                .build();
        HarvestResponse response = HarvestResponse.builder().id(100).build();

        when(seasonRepository.findById(10)).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(harvestRepository.save(any(Harvest.class))).thenReturn(saved);
        when(farmAccessService.getCurrentUserId()).thenReturn(actor.getId());
        when(harvestRepository.findAllBySeason_Id(10)).thenReturn(java.util.List.of(saved));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(harvestMapper.toResponse(saved)).thenReturn(response);

        seasonHarvestService.createHarvest(10, request);

        ArgumentCaptor<ReceiveHarvestRequest> receiveRequestCaptor = ArgumentCaptor.forClass(ReceiveHarvestRequest.class);
        verify(inventoryCommandPort).receiveFromHarvest(eq(100), eq(actor.getId()), receiveRequestCaptor.capture());
        ReceiveHarvestRequest sentRequest = receiveRequestCaptor.getValue();
        assertNotNull(sentRequest);
        assertEquals(7, sentRequest.getWarehouseId());
        assertEquals("Rice", sentRequest.getProductName());
        assertEquals("LOT-20260310-01", sentRequest.getLotCode());
    }

    @Test
    @DisplayName("updateHarvest is blocked when harvest already received to product warehouse")
    void updateHarvest_whenReceived_throwsError() {
        Farm farm = Farm.builder().id(1).build();
        Plot plot = Plot.builder().id(2).farm(farm).build();
        Season season = Season.builder()
                .id(10)
                .plot(plot)
                .status(SeasonStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 30))
                .build();
        Harvest harvest = Harvest.builder()
                .id(100)
                .season(season)
                .harvestDate(LocalDate.of(2026, 3, 10))
                .quantity(BigDecimal.valueOf(20))
                .unit(BigDecimal.ONE)
                .build();

        UpdateHarvestDetailRequest request = UpdateHarvestDetailRequest.builder()
                .harvestDate(LocalDate.of(2026, 3, 11))
                .quantity(BigDecimal.valueOf(25))
                .unit(BigDecimal.ONE)
                .grade("A")
                .note("updated")
                .build();

        when(harvestRepository.findById(100)).thenReturn(Optional.of(harvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(inventoryQueryPort.existsProductWarehouseLotByHarvestId(100)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> seasonHarvestService.updateHarvest(100, request));
        assertEquals(ErrorCode.HARVEST_ALREADY_RECEIVED_TO_PRODUCT_WAREHOUSE, exception.getErrorCode());
        verify(harvestRepository).findById(eq(100));
    }
}
