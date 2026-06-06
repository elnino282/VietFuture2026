package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryCommandPort;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.inventory.port.ReceiveHarvestRequest;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    ProductWarehouseLotRepository productWarehouseLotRepository;
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
    @DisplayName("updateHarvest syncs linked product warehouse lot")
    void updateHarvest_whenReceived_syncsLinkedProductWarehouseLot() {
        Season season = buildActiveSeason(10);
        Harvest harvest = buildHarvest(100, season);
        ProductWarehouseLot linkedLot = buildLot(harvest, ProductWarehouseLotStatus.IN_STOCK, "15");

        UpdateHarvestDetailRequest request = UpdateHarvestDetailRequest.builder()
                .harvestDate(LocalDate.of(2026, 3, 11))
                .quantity(BigDecimal.valueOf(25))
                .unit(BigDecimal.ONE)
                .grade("A")
                .note("updated")
                .build();

        when(harvestRepository.findById(100)).thenReturn(Optional.of(harvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.of(linkedLot));
        when(harvestRepository.save(any(Harvest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productWarehouseLotRepository.save(any(ProductWarehouseLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(harvestRepository.findAllBySeason_Id(10)).thenReturn(List.of(harvest));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(harvestMapper.toResponse(harvest)).thenReturn(HarvestResponse.builder().id(100).build());

        HarvestResponse response = seasonHarvestService.updateHarvest(100, request);

        assertEquals("stored", response.getStatus());
        assertEquals(LocalDate.of(2026, 3, 11), linkedLot.getHarvestedAt());
        assertEquals(BigDecimal.valueOf(25), linkedLot.getInitialQuantity());
        assertEquals(BigDecimal.valueOf(20), linkedLot.getOnHandQuantity());
        assertEquals("A", linkedLot.getGrade());
        assertEquals("updated", linkedLot.getNote());
        verify(productWarehouseLotRepository).save(linkedLot);
    }

    @Test
    @DisplayName("updateHarvest rejects quantity lower than stock already moved out")
    void updateHarvest_whenQuantityBelowMovedOut_throwsInsufficientStock() {
        Season season = buildActiveSeason(10);
        Harvest harvest = buildHarvest(100, season);
        ProductWarehouseLot linkedLot = buildLot(harvest, ProductWarehouseLotStatus.IN_STOCK, "5");

        UpdateHarvestDetailRequest request = UpdateHarvestDetailRequest.builder()
                .harvestDate(LocalDate.of(2026, 3, 11))
                .quantity(BigDecimal.valueOf(10))
                .unit(BigDecimal.ONE)
                .grade("A")
                .note("updated")
                .build();

        when(harvestRepository.findById(100)).thenReturn(Optional.of(harvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.of(linkedLot));

        AppException exception = assertThrows(AppException.class,
                () -> seasonHarvestService.updateHarvest(100, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
        verify(harvestRepository).findById(eq(100));
    }

    @Test
    @DisplayName("getHarvest derives stored status from in-stock linked lot")
    void getHarvest_withInStockLot_returnsStoredStatus() {
        Harvest harvest = buildHarvest(100);
        ProductWarehouseLot lot = buildLot(harvest, ProductWarehouseLotStatus.IN_STOCK, "20");

        when(harvestRepository.findById(100)).thenReturn(Optional.of(harvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(harvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.of(lot));
        when(harvestMapper.toResponse(harvest)).thenReturn(HarvestResponse.builder().id(100).build());

        HarvestResponse response = seasonHarvestService.getHarvest(100);

        assertEquals("stored", response.getStatus());
    }

    @Test
    @DisplayName("getHarvest derives processing status from held linked lot")
    void getHarvest_withHeldLot_returnsProcessingStatus() {
        Harvest harvest = buildHarvest(100);
        ProductWarehouseLot lot = buildLot(harvest, ProductWarehouseLotStatus.HOLD, "20");

        when(harvestRepository.findById(100)).thenReturn(Optional.of(harvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(harvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.of(lot));
        when(harvestMapper.toResponse(harvest)).thenReturn(HarvestResponse.builder().id(100).build());

        HarvestResponse response = seasonHarvestService.getHarvest(100);

        assertEquals("processing", response.getStatus());
    }

    @Test
    @DisplayName("getHarvest derives sold status from depleted or zero stock linked lot")
    void getHarvest_withDepletedOrZeroStockLot_returnsSoldStatus() {
        Harvest depletedHarvest = buildHarvest(100);
        ProductWarehouseLot depletedLot = buildLot(depletedHarvest, ProductWarehouseLotStatus.DEPLETED, "0");

        when(harvestRepository.findById(100)).thenReturn(Optional.of(depletedHarvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(depletedHarvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.of(depletedLot));
        when(harvestMapper.toResponse(depletedHarvest)).thenReturn(HarvestResponse.builder().id(100).build());

        HarvestResponse depletedResponse = seasonHarvestService.getHarvest(100);

        assertEquals("sold", depletedResponse.getStatus());

        Harvest zeroStockHarvest = buildHarvest(101);
        ProductWarehouseLot zeroStockLot = buildLot(zeroStockHarvest, ProductWarehouseLotStatus.IN_STOCK, "0");

        when(harvestRepository.findById(101)).thenReturn(Optional.of(zeroStockHarvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(zeroStockHarvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(101)).thenReturn(Optional.of(zeroStockLot));
        when(harvestMapper.toResponse(zeroStockHarvest)).thenReturn(HarvestResponse.builder().id(101).build());

        HarvestResponse zeroStockResponse = seasonHarvestService.getHarvest(101);

        assertEquals("sold", zeroStockResponse.getStatus());
    }

    @Test
    @DisplayName("getHarvest returns null status when linked lot is missing or archived")
    void getHarvest_withMissingOrArchivedLot_returnsNullStatus() {
        Harvest missingLotHarvest = buildHarvest(100);

        when(harvestRepository.findById(100)).thenReturn(Optional.of(missingLotHarvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(missingLotHarvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(100)).thenReturn(Optional.empty());
        when(harvestMapper.toResponse(missingLotHarvest)).thenReturn(HarvestResponse.builder().id(100).build());

        HarvestResponse missingLotResponse = seasonHarvestService.getHarvest(100);

        assertEquals(null, missingLotResponse.getStatus());

        Harvest archivedHarvest = buildHarvest(101);
        ProductWarehouseLot archivedLot = buildLot(archivedHarvest, ProductWarehouseLotStatus.ARCHIVED, "20");

        when(harvestRepository.findById(101)).thenReturn(Optional.of(archivedHarvest));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(archivedHarvest.getSeason());
        when(productWarehouseLotRepository.findByHarvest_Id(101)).thenReturn(Optional.of(archivedLot));
        when(harvestMapper.toResponse(archivedHarvest)).thenReturn(HarvestResponse.builder().id(101).build());

        HarvestResponse archivedResponse = seasonHarvestService.getHarvest(101);

        assertEquals(null, archivedResponse.getStatus());
    }

    @Test
    @DisplayName("listHarvestsForSeason hydrates statuses with batch lot repository")
    void listHarvestsForSeason_hydratesStatusesWithBatchLotRepository() {
        Season season = buildActiveSeason(10);
        Harvest storedHarvest = buildHarvest(100, season);
        Harvest soldHarvest = buildHarvest(101, season);
        ProductWarehouseLot storedLot = buildLot(storedHarvest, ProductWarehouseLotStatus.IN_STOCK, "20");
        ProductWarehouseLot soldLot = buildLot(soldHarvest, ProductWarehouseLotStatus.DEPLETED, "0");

        when(seasonRepository.findById(10)).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(harvestRepository.findAllBySeason_Id(10)).thenReturn(List.of(storedHarvest, soldHarvest));
        when(productWarehouseLotRepository.findAllByHarvest_IdIn(List.of(101, 100)))
                .thenReturn(List.of(soldLot, storedLot));
        when(harvestMapper.toResponse(storedHarvest)).thenReturn(HarvestResponse.builder().id(100).build());
        when(harvestMapper.toResponse(soldHarvest)).thenReturn(HarvestResponse.builder().id(101).build());

        PageResponse<HarvestResponse> response = seasonHarvestService.listHarvestsForSeason(
                10,
                null,
                null,
                0,
                20);

        assertEquals(2, response.getItems().size());
        assertEquals("sold", response.getItems().get(0).getStatus());
        assertEquals("stored", response.getItems().get(1).getStatus());
        verify(productWarehouseLotRepository).findAllByHarvest_IdIn(List.of(101, 100));
        verify(productWarehouseLotRepository, never()).findByHarvest_Id(any());
    }

    private Season buildActiveSeason(Integer id) {
        Farm farm = Farm.builder().id(1).build();
        Plot plot = Plot.builder().id(2).farm(farm).build();
        return Season.builder()
                .id(id)
                .plot(plot)
                .status(SeasonStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .plannedHarvestDate(LocalDate.of(2026, 3, 30))
                .build();
    }

    private Harvest buildHarvest(Integer id) {
        return buildHarvest(id, buildActiveSeason(10));
    }

    private Harvest buildHarvest(Integer id, Season season) {
        return Harvest.builder()
                .id(id)
                .season(season)
                .harvestDate(LocalDate.of(2026, 3, 10))
                .quantity(BigDecimal.valueOf(20))
                .unit(BigDecimal.ONE)
                .build();
    }

    private ProductWarehouseLot buildLot(
            Harvest harvest,
            ProductWarehouseLotStatus status,
            String onHandQuantity) {
        return ProductWarehouseLot.builder()
                .harvest(harvest)
                .status(status)
                .onHandQuantity(new BigDecimal(onHandQuantity))
                .initialQuantity(BigDecimal.valueOf(20))
                .build();
    }
}
