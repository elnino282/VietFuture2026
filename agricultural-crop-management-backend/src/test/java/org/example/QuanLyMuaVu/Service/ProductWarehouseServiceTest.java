package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.service.FarmAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.module.inventory.dto.request.AdjustProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.CreateProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.request.StockOutProductWarehouseLotRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockLocationRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.inventory.service.ProductWarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWarehouseServiceTest {

    @Mock
    ProductWarehouseLotRepository productWarehouseLotRepository;
    @Mock
    ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    @Mock
    SeasonQueryPort seasonQueryPort;
    @Mock
    FarmQueryPort farmQueryPort;
    @Mock
    HarvestQueryPort harvestQueryPort;
    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    StockLocationRepository stockLocationRepository;
    @Mock
    FarmAccessService farmAccessService;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    ProductWarehouseService productWarehouseService;

    private User actor;
    private Farm farm;
    private Plot plot;
    private Season season;
    private Warehouse warehouse;
    private StockLocation location;
    private Harvest harvest;

    @BeforeEach
    void setUp() {
        actor = User.builder().id(2L).username("farmer").fullName("Farmer A").build();

        farm = Farm.builder().id(1).name("Farm 1").build();
        plot = Plot.builder().id(10).plotName("Plot 10").farm(farm).build();
        season = Season.builder()
                .id(100)
                .seasonName("Season 2026")
                .plot(plot)
                .crop(Crop.builder().id(5).cropName("Rice").build())
                .build();
        warehouse = Warehouse.builder().id(8).farm(farm).name("Output Warehouse").type("OUTPUT").build();
        location = StockLocation.builder().id(80).warehouse(warehouse).zone("A").aisle("1").build();
        harvest = Harvest.builder()
                .id(999)
                .season(season)
                .harvestDate(LocalDate.of(2026, 3, 10))
                .quantity(BigDecimal.valueOf(120))
                .grade("A")
                .note("good harvest")
                .build();
    }

    @Test
    @DisplayName("receiveFromHarvest creates lot and receipt transaction")
    void receiveFromHarvest_createsLotAndTransaction() {
        when(productWarehouseLotRepository.findByHarvest_Id(999)).thenReturn(Optional.empty());
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(warehouseRepository.findAllByFarm(farm)).thenReturn(List.of(warehouse));
        when(stockLocationRepository.findAllByWarehouse(warehouse)).thenReturn(List.of(location));
        when(productWarehouseLotRepository.findByLotCode(any())).thenReturn(Optional.empty());
        when(productWarehouseLotRepository.save(any(ProductWarehouseLot.class))).thenAnswer(invocation -> {
            ProductWarehouseLot lot = invocation.getArgument(0);
            lot.setId(1);
            return lot;
        });
        when(productWarehouseTransactionRepository.save(any(ProductWarehouseTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductWarehouseLot result = productWarehouseService.receiveFromHarvest(harvest, actor);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(BigDecimal.valueOf(120), result.getOnHandQuantity());
        verify(productWarehouseLotRepository).save(any(ProductWarehouseLot.class));
        verify(productWarehouseTransactionRepository).save(any(ProductWarehouseTransaction.class));
    }

    @Test
    @DisplayName("adjustLot throws INSUFFICIENT_STOCK when result is negative")
    void adjustLot_whenNegativeResult_throwsInsufficientStock() {
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(22)
                .farm(farm)
                .warehouse(warehouse)
                .onHandQuantity(BigDecimal.valueOf(5))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .unit("kg")
                .build();
        when(productWarehouseLotRepository.findByIdForUpdate(22)).thenReturn(Optional.of(lot));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);

        AdjustProductWarehouseLotRequest request = AdjustProductWarehouseLotRequest.builder()
                .quantityDelta(BigDecimal.valueOf(-10))
                .note("inventory correction")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> productWarehouseService.adjustLot(22, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
    }

    @Test
    @DisplayName("adjustLot writes adjustment transaction with consistent resulting_on_hand")
    void adjustLot_recordsTransactionWithConsistentResultingOnHand() {
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(22)
                .farm(farm)
                .warehouse(warehouse)
                .onHandQuantity(BigDecimal.valueOf(10))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .unit("kg")
                .build();
        when(productWarehouseLotRepository.findByIdForUpdate(22)).thenReturn(Optional.of(lot));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);
        when(farmAccessService.getCurrentUser()).thenReturn(actor);
        when(productWarehouseLotRepository.save(any(ProductWarehouseLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productWarehouseTransactionRepository.save(any(ProductWarehouseTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdjustProductWarehouseLotRequest request = AdjustProductWarehouseLotRequest.builder()
                .quantityDelta(BigDecimal.valueOf(3))
                .note("inventory correction")
                .build();

        productWarehouseService.adjustLot(22, request);

        ArgumentCaptor<ProductWarehouseTransaction> transactionCaptor = ArgumentCaptor.forClass(ProductWarehouseTransaction.class);
        verify(productWarehouseTransactionRepository).save(transactionCaptor.capture());
        ProductWarehouseTransaction transaction = transactionCaptor.getValue();

        assertEquals(ProductWarehouseTransactionType.ADJUSTMENT, transaction.getTransactionType());
        assertEquals(BigDecimal.valueOf(3), transaction.getQuantity());
        assertEquals(BigDecimal.valueOf(13), transaction.getResultingOnHand());
        assertEquals(BigDecimal.valueOf(13), lot.getOnHandQuantity());
    }

    @Test
    @DisplayName("stockOutLot throws INSUFFICIENT_STOCK when requested quantity is greater than on hand")
    void stockOutLot_whenQuantityGreaterThanOnHand_throwsInsufficientStock() {
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(55)
                .farm(farm)
                .warehouse(warehouse)
                .onHandQuantity(BigDecimal.valueOf(5))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .unit("kg")
                .build();
        when(productWarehouseLotRepository.findByIdForUpdate(55)).thenReturn(Optional.of(lot));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);
        when(farmAccessService.getCurrentUser()).thenReturn(actor);

        StockOutProductWarehouseLotRequest request = StockOutProductWarehouseLotRequest.builder()
                .quantity(BigDecimal.valueOf(6))
                .note("ship order")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> productWarehouseService.stockOutLot(55, request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
    }

    @Test
    @DisplayName("stockOutLot writes stock-out transaction with consistent resulting_on_hand")
    void stockOutLot_recordsTransactionWithConsistentResultingOnHand() {
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(56)
                .farm(farm)
                .warehouse(warehouse)
                .onHandQuantity(BigDecimal.valueOf(20))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .unit("kg")
                .build();
        when(productWarehouseLotRepository.findByIdForUpdate(56)).thenReturn(Optional.of(lot));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);
        when(farmAccessService.getCurrentUser()).thenReturn(actor);
        when(productWarehouseLotRepository.save(any(ProductWarehouseLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productWarehouseTransactionRepository.save(any(ProductWarehouseTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockOutProductWarehouseLotRequest request = StockOutProductWarehouseLotRequest.builder()
                .quantity(BigDecimal.valueOf(7))
                .note("ship order")
                .build();

        productWarehouseService.stockOutLot(56, request);

        ArgumentCaptor<ProductWarehouseTransaction> transactionCaptor = ArgumentCaptor.forClass(ProductWarehouseTransaction.class);
        verify(productWarehouseTransactionRepository).save(transactionCaptor.capture());
        ProductWarehouseTransaction transaction = transactionCaptor.getValue();

        assertEquals(ProductWarehouseTransactionType.STOCK_OUT, transaction.getTransactionType());
        assertEquals(BigDecimal.valueOf(7), transaction.getQuantity());
        assertEquals(BigDecimal.valueOf(13), transaction.getResultingOnHand());
        assertEquals(BigDecimal.valueOf(13), lot.getOnHandQuantity());
    }

    @Test
    @DisplayName("createLot throws PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE when harvest already linked")
    void createLot_whenHarvestAlreadyLinked_throwsDuplicate() {
        CreateProductWarehouseLotRequest request = CreateProductWarehouseLotRequest.builder()
                .productName("Rice")
                .farmId(1)
                .plotId(10)
                .seasonId(100)
                .harvestId(999)
                .warehouseId(8)
                .harvestedAt(LocalDate.of(2026, 3, 10))
                .unit("kg")
                .initialQuantity(BigDecimal.valueOf(50))
                .build();

        when(farmQueryPort.findFarmById(1)).thenReturn(Optional.of(farm));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);
        when(farmQueryPort.findPlotById(10)).thenReturn(Optional.of(plot));
        when(seasonQueryPort.findSeasonById(100)).thenReturn(Optional.of(season));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessSeason(season);
        when(harvestQueryPort.findHarvestById(999)).thenReturn(Optional.of(harvest));
        when(productWarehouseLotRepository.existsByHarvest_Id(999)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> productWarehouseService.createLot(request));
        assertEquals(ErrorCode.PRODUCT_WAREHOUSE_RECEIPT_DUPLICATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("archiveLot marks lot as ARCHIVED")
    void archiveLot_marksStatusArchived() {
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(77)
                .farm(farm)
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .build();

        when(productWarehouseLotRepository.findByIdForUpdate(77)).thenReturn(Optional.of(lot));
        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(farm);
        when(productWarehouseLotRepository.save(any(ProductWarehouseLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productWarehouseService.archiveLot(77);

        assertEquals(ProductWarehouseLotStatus.ARCHIVED, lot.getStatus());
        verify(productWarehouseLotRepository).save(lot);
    }
}


