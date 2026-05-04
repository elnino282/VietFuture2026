package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.service.FarmAccessService;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.inventory.dto.request.RecordStockMovementRequest;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockLocationResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.StockMovementResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.WarehouseResponse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockLocationRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.example.QuanLyMuaVu.module.inventory.service.InventoryService;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for InventoryService.
 * 
 * Tests cover:
 * - GET MY WAREHOUSES: Happy path, empty list
 * - GET LOCATIONS BY WAREHOUSE: Happy path, warehouse not found
 * - RECORD MOVEMENT: All types (IN, OUT, ADJUST), validations
 * - GET ON-HAND QUANTITY: Happy path, not found cases
 * - Business rules: OUT requires season, ADJUST requires note, stock validation
 * 
 * Follows AAA (Arrange-Act-Assert) pattern.
 */
@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

        @Mock
        private WarehouseRepository warehouseRepository;

        @Mock
        private StockLocationRepository stockLocationRepository;

        @Mock
        private SupplyLotRepository supplyLotRepository;

        @Mock
        private StockMovementRepository stockMovementRepository;

        @Mock
        private InventoryBalanceRepository inventoryBalanceRepository;

        @Mock
        private FarmQueryPort farmQueryPort;

        @Mock
        private SeasonQueryPort seasonQueryPort;

        @Mock
        private TaskQueryPort taskQueryPort;

        @Mock
        private FarmAccessService farmAccessService;

        @Mock
        private AuditLogService auditLogService;

        @InjectMocks
        private InventoryService inventoryService;

        // Test fixtures
        private User testUser;
        private Farm testFarm;
        private Warehouse testWarehouse;
        private StockLocation testLocation;
        private SupplyLot testLot;
        private SupplyItem testSupplyItem;
        private Season testSeason;
        private Plot testPlot;
        private Task testTask;

        @BeforeEach
        void setUp() {
                // Arrange: Create test data hierarchy
                testUser = User.builder()
                                .id(1L)
                                .username("farmer")
                                .email("farmer@test.com")
                                .build();

                testFarm = Farm.builder()
                                .id(1)
                                .name("Test Farm")
                                .user(testUser)
                                .area(BigDecimal.valueOf(100))
                                .active(true)
                                .build();

                testWarehouse = Warehouse.builder()
                                .id(1)
                                .name("Main Warehouse")
                                .type("STORAGE")
                                .farm(testFarm)
                                .build();

                testLocation = StockLocation.builder()
                                .id(1)
                                .warehouse(testWarehouse)
                                .zone("A")
                                .aisle("1")
                                .shelf("2")
                                .bin("3")
                                .build();

                testSupplyItem = SupplyItem.builder()
                                .id(1)
                                .name("Fertilizer NPK")
                                .unit("kg")
                                .build();

                testLot = SupplyLot.builder()
                                .id(1)
                                .batchCode("LOT-2024-001")
                                .supplyItem(testSupplyItem)
                                .status("IN_STOCK")
                                .build();

                testPlot = Plot.builder()
                                .id(1)
                                .plotName("Test Plot")
                                .farm(testFarm)
                                .user(testUser)
                                .area(BigDecimal.valueOf(10))
                                .build();

                testSeason = Season.builder()
                                .id(1)
                                .seasonName("Spring 2024")
                                .plot(testPlot)
                                .build();

                testTask = Task.builder()
                                .id(1)
                                .title("Apply Fertilizer")
                                .season(testSeason)
                                .build();

                lenient().when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));
                lenient().when(inventoryBalanceRepository.existsBySupplyLot_IdAndWarehouse_Farm_IdIn(anyInt(), anyList()))
                                .thenReturn(true);
                lenient().when(inventoryBalanceRepository.existsBySupplyLot_Id(anyInt())).thenReturn(true);
                lenient().when(inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(any(), any(), any()))
                                .thenReturn(Optional.empty());
                lenient().when(inventoryBalanceRepository.findAllByLotAndWarehouseWithLock(any(), any()))
                                .thenReturn(new java.util.ArrayList<>());
                lenient().when(inventoryBalanceRepository.save(any(InventoryBalance.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                lenient().when(inventoryBalanceRepository.sumQuantityByLotAndWarehouse(any(), any()))
                                .thenReturn(BigDecimal.ZERO);
                lenient().when(inventoryBalanceRepository.getCurrentQuantity(any(), any(), any()))
                                .thenReturn(BigDecimal.ZERO);
                lenient().when(stockMovementRepository.existsBySupplyLot_Id(anyInt())).thenReturn(true);
                lenient().when(stockMovementRepository.existsBySupplyLot_IdAndWarehouse_Farm_IdIn(anyInt(), anyList()))
                                .thenReturn(true);
        }

        // =========================================================================
        // GET MY WAREHOUSES TESTS
        // =========================================================================

        @Nested
        @DisplayName("getMyWarehouses() Tests")
        class GetMyWarehousesTests {

                @Test
                @DisplayName("Happy Path: Returns list of warehouses for accessible farms")
                void getMyWarehouses_WithAccessibleFarms_ReturnsWarehouses() {
                        // Arrange
                        when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));
                        when(farmQueryPort.findFarmsByIds(List.of(1))).thenReturn(List.of(testFarm));
                        when(warehouseRepository.findByFarmIn(List.of(testFarm))).thenReturn(List.of(testWarehouse));

                        // Act
                        List<WarehouseResponse> result = inventoryService.getMyWarehouses(null);

                        // Assert
                        assertNotNull(result);
                        assertEquals(1, result.size());
                        assertEquals("Main Warehouse", result.get(0).getName());
                        assertEquals(1, result.get(0).getFarmId());
                        assertEquals("Test Farm", result.get(0).getFarmName());
                }

                @Test
                @DisplayName("Edge Case: Returns empty list when no accessible farms")
                void getMyWarehouses_WithNoAccessibleFarms_ReturnsEmptyList() {
                        // Arrange
                        when(farmAccessService.getAccessibleFarmIdsForCurrentUser())
                                        .thenReturn(Collections.emptyList());

                        // Act
                        List<WarehouseResponse> result = inventoryService.getMyWarehouses(null);

                        // Assert
                        assertNotNull(result);
                        assertTrue(result.isEmpty());
                        verify(farmQueryPort, never()).findFarmsByIds(any());
                }

                @Test
                @DisplayName("Edge Case: Returns multiple warehouses")
                void getMyWarehouses_WithMultipleWarehouses_ReturnsAll() {
                        // Arrange
                        Warehouse warehouse2 = Warehouse.builder()
                                        .id(2)
                                        .name("Cold Storage")
                                        .type("COLD")
                                        .farm(testFarm)
                                        .build();

                        when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));
                        when(farmQueryPort.findFarmsByIds(List.of(1))).thenReturn(List.of(testFarm));
                        when(warehouseRepository.findByFarmIn(List.of(testFarm)))
                                        .thenReturn(List.of(testWarehouse, warehouse2));

                        // Act
                        List<WarehouseResponse> result = inventoryService.getMyWarehouses(null);

                        // Assert
                        assertEquals(2, result.size());
                }
        }

        // =========================================================================
        // GET LOCATIONS BY WAREHOUSE TESTS
        // =========================================================================

        @Nested
        @DisplayName("getLocationsByWarehouse() Tests")
        class GetLocationsByWarehouseTests {

                @Test
                @DisplayName("Happy Path: Returns locations for valid warehouse")
                void getLocationsByWarehouse_WithValidWarehouse_ReturnsLocations() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(stockLocationRepository.findAllByWarehouse(testWarehouse))
                                        .thenReturn(List.of(testLocation));

                        // Act
                        List<StockLocationResponse> result = inventoryService.getLocationsByWarehouse(1);

                        // Assert
                        assertNotNull(result);
                        assertEquals(1, result.size());
                        assertEquals("A", result.get(0).getZone());
                        assertEquals("1", result.get(0).getAisle());
                        assertEquals("2", result.get(0).getShelf());
                        assertEquals("3", result.get(0).getBin());
                        // Label should be "A-1-2-3"
                        assertEquals("A-1-2-3", result.get(0).getLabel());
                }

                @Test
                @DisplayName("Negative Case: Throws WAREHOUSE_NOT_FOUND when warehouse doesn't exist")
                void getLocationsByWarehouse_WhenWarehouseNotFound_ThrowsException() {
                        // Arrange
                        when(warehouseRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.getLocationsByWarehouse(999));

                        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Edge Case: Returns empty list when no locations")
                void getLocationsByWarehouse_WithNoLocations_ReturnsEmptyList() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(stockLocationRepository.findAllByWarehouse(testWarehouse))
                                        .thenReturn(Collections.emptyList());

                        // Act
                        List<StockLocationResponse> result = inventoryService.getLocationsByWarehouse(1);

                        // Assert
                        assertNotNull(result);
                        assertTrue(result.isEmpty());
                }
        }

        // =========================================================================
        // RECORD MOVEMENT TESTS - IN
        // =========================================================================

        @Nested
        @DisplayName("recordMovement() - IN Type Tests")
        class RecordMovementInTests {

                @Test
                @DisplayName("Happy Path: Records IN movement successfully")
                void recordMovement_InType_RecordsSuccessfully() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(100))
                                        .note("Initial stock")
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(1)).thenReturn(Optional.of(testLocation));

                        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
                        when(stockMovementRepository.save(captor.capture())).thenAnswer(invocation -> {
                                StockMovement m = invocation.getArgument(0);
                                m.setId(1);
                                return m;
                        });

                        // Act
                        StockMovementResponse response = inventoryService.recordMovement(request);

                        // Assert
                        assertNotNull(response);

                        StockMovement captured = captor.getValue();
                        assertEquals(StockMovementType.IN, captured.getMovementType());
                        assertEquals(BigDecimal.valueOf(100), captured.getQuantity());
                        assertEquals(testWarehouse, captured.getWarehouse());
                        assertEquals(testLocation, captured.getLocation());
                        assertEquals(testLot, captured.getSupplyLot());
                }

                @Test
                @DisplayName("Happy Path: Records IN movement without location")
                void recordMovement_InTypeWithoutLocation_RecordsSuccessfully() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(null)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(50))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockMovementRepository.save(any())).thenAnswer(i -> {
                                StockMovement m = i.getArgument(0);
                                m.setId(1);
                                return m;
                        });

                        // Act
                        StockMovementResponse response = inventoryService.recordMovement(request);

                        // Assert
                        assertNotNull(response);
                        verify(stockLocationRepository, never()).findById(any());
                }
        }

        // =========================================================================
        // RECORD MOVEMENT TESTS - OUT
        // =========================================================================

        @Nested
        @DisplayName("recordMovement() - OUT Type Tests")
        class RecordMovementOutTests {

                @Test
                @DisplayName("Happy Path: Records OUT movement with season")
                void recordMovement_OutTypeWithSeason_RecordsSuccessfully() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(10))
                                        .seasonId(1)
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
                        when(stockMovementRepository.calculateOnHandQuantity(testLot, testWarehouse, null))
                                        .thenReturn(BigDecimal.valueOf(100)); // Sufficient stock
                        when(stockMovementRepository.save(any())).thenAnswer(i -> {
                                StockMovement m = i.getArgument(0);
                                m.setId(1);
                                return m;
                        });

                        // Act
                        StockMovementResponse response = inventoryService.recordMovement(request);

                        // Assert
                        assertNotNull(response);
                        verify(seasonQueryPort).findSeasonById(1);
                }

                @Test
                @DisplayName("Negative Case: OUT without season throws OUT_SEASON_REQUIRED")
                void recordMovement_OutTypeWithoutSeason_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(10))
                                        .seasonId(null) // No season
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.OUT_SEASON_REQUIRED, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: OUT with lot not IN_STOCK throws LOT_NOT_IN_STOCK")
                void recordMovement_OutTypeWithNotInStockLot_ThrowsException() {
                        // Arrange
                        SupplyLot expiredLot = SupplyLot.builder()
                                        .id(2)
                                        .batchCode("LOT-EXPIRED")
                                        .supplyItem(testSupplyItem)
                                        .status("EXPIRED") // Not in stock
                                        .build();

                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(2)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(10))
                                        .seasonId(1)
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(2)).thenReturn(Optional.of(expiredLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.LOT_NOT_IN_STOCK, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: OUT with insufficient stock throws INSUFFICIENT_STOCK")
                void recordMovement_OutTypeWithInsufficientStock_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(1000)) // Requesting more than available
                                        .seasonId(1)
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
                        when(stockMovementRepository.calculateOnHandQuantity(testLot, testWarehouse, null))
                                        .thenReturn(BigDecimal.valueOf(50)); // Only 50 available

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
                }
        }

        // =========================================================================
        // RECORD MOVEMENT TESTS - ADJUST
        // =========================================================================

        @Nested
        @DisplayName("recordMovement() - ADJUST Type Tests")
        class RecordMovementAdjustTests {

                @Test
                @DisplayName("Happy Path: Records ADJUST movement with note")
                void recordMovement_AdjustTypeWithNote_RecordsSuccessfully() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("ADJUST")
                                        .quantity(BigDecimal.valueOf(5))
                                        .note("Inventory reconciliation - found extra items")
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockMovementRepository.save(any())).thenAnswer(i -> {
                                StockMovement m = i.getArgument(0);
                                m.setId(1);
                                return m;
                        });
                        when(farmAccessService.getCurrentUser()).thenReturn(testUser);

                        // Act
                        StockMovementResponse response = inventoryService.recordMovement(request);

                        // Assert
                        assertNotNull(response);
                        verify(auditLogService).logModuleOperation(
                                        eq("INVENTORY"),
                                        eq("STOCK_MOVEMENT"),
                                        eq(1),
                                        eq("INVENTORY_ADJUSTED"),
                                        eq("farmer"),
                                        any(),
                                        eq("Inventory reconciliation - found extra items"),
                                        isNull());
                }

                @Test
                @DisplayName("Negative Case: ADJUST without note throws ADJUST_NOTE_REQUIRED")
                void recordMovement_AdjustTypeWithoutNote_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("ADJUST")
                                        .quantity(BigDecimal.valueOf(5))
                                        .note(null) // No note
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.ADJUST_NOTE_REQUIRED, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: ADJUST with blank note throws ADJUST_NOTE_REQUIRED")
                void recordMovement_AdjustTypeWithBlankNote_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("ADJUST")
                                        .quantity(BigDecimal.valueOf(5))
                                        .note("   ") // Blank note
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.ADJUST_NOTE_REQUIRED, exception.getErrorCode());
                }
        }

        // =========================================================================
        // RECORD MOVEMENT - GENERAL VALIDATION TESTS
        // =========================================================================

        @Nested
        @DisplayName("recordMovement() - General Validation Tests")
        class RecordMovementValidationTests {

                @Test
                @DisplayName("Negative Case: Throws WAREHOUSE_NOT_FOUND when warehouse doesn't exist")
                void recordMovement_WhenWarehouseNotFound_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(999)
                                        .supplyLotId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .build();

                        when(warehouseRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws SUPPLY_LOT_NOT_FOUND when lot doesn't exist")
                void recordMovement_WhenLotNotFound_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(999)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.SUPPLY_LOT_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws LOCATION_NOT_FOUND when location doesn't exist")
                void recordMovement_WhenLocationNotFound_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(999)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.LOCATION_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws BAD_REQUEST when location belongs to different warehouse")
                void recordMovement_WhenLocationBelongsToDifferentWarehouse_ThrowsException() {
                        // Arrange
                        Warehouse otherWarehouse = Warehouse.builder().id(2).name("Other").farm(testFarm).build();
                        StockLocation locationInOtherWarehouse = StockLocation.builder()
                                        .id(2)
                                        .warehouse(otherWarehouse)
                                        .zone("B")
                                        .build();

                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1) // Warehouse 1
                                        .supplyLotId(1)
                                        .locationId(2) // Location in warehouse 2
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(2)).thenReturn(Optional.of(locationInOtherWarehouse));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws BAD_REQUEST when quantity is zero")
                void recordMovement_WhenQuantityZero_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.ZERO)
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws BAD_REQUEST when quantity is negative")
                void recordMovement_WhenQuantityNegative_ThrowsException() {
                        // Arrange
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(-10))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
                }
        }

        // =========================================================================
        // INVENTORY BALANCE CONSISTENCY TESTS
        // =========================================================================

        @Nested
        @DisplayName("recordMovement() - Inventory Balance Consistency")
        class RecordMovementBalanceConsistencyTests {

                @Test
                @DisplayName("IN movement updates inventory balance by location")
                void recordMovement_InMovement_updatesInventoryBalance() {
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .note("Receive supply")
                                        .build();

                        InventoryBalance currentBalance = InventoryBalance.builder()
                                        .id(10L)
                                        .supplyLot(testLot)
                                        .warehouse(testWarehouse)
                                        .location(testLocation)
                                        .quantity(BigDecimal.valueOf(5))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(1)).thenReturn(Optional.of(testLocation));
                        when(inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(testLot, testWarehouse, testLocation))
                                        .thenReturn(Optional.of(currentBalance));
                        when(stockMovementRepository.save(any())).thenAnswer(invocation -> {
                                StockMovement movement = invocation.getArgument(0);
                                movement.setId(111);
                                return movement;
                        });

                        inventoryService.recordMovement(request);

                        ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
                        verify(inventoryBalanceRepository).save(balanceCaptor.capture());
                        assertEquals(BigDecimal.valueOf(15), balanceCaptor.getValue().getQuantity());
                }

                @Test
                @DisplayName("OUT movement deducts balance and keeps stock consistent")
                void recordMovement_OutMovement_deductsInventoryBalance() {
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(1)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(3))
                                        .seasonId(1)
                                        .note("Use in task")
                                        .build();

                        InventoryBalance currentBalance = InventoryBalance.builder()
                                        .id(11L)
                                        .supplyLot(testLot)
                                        .warehouse(testWarehouse)
                                        .location(testLocation)
                                        .quantity(BigDecimal.valueOf(8))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(1)).thenReturn(Optional.of(testLocation));
                        when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
                        when(inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(testLot, testWarehouse, testLocation))
                                        .thenReturn(Optional.of(currentBalance));
                        when(stockMovementRepository.save(any())).thenAnswer(invocation -> {
                                StockMovement movement = invocation.getArgument(0);
                                movement.setId(112);
                                return movement;
                        });

                        inventoryService.recordMovement(request);

                        ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
                        verify(inventoryBalanceRepository).save(balanceCaptor.capture());
                        assertEquals(BigDecimal.valueOf(5), balanceCaptor.getValue().getQuantity());
                }

                @Test
                @DisplayName("OUT movement cannot exceed inventory balance")
                void recordMovement_OutMovement_exceedingBalance_throwsInsufficientStock() {
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .locationId(1)
                                        .movementType("OUT")
                                        .quantity(BigDecimal.valueOf(9))
                                        .seasonId(1)
                                        .note("Use in task")
                                        .build();

                        InventoryBalance currentBalance = InventoryBalance.builder()
                                        .id(12L)
                                        .supplyLot(testLot)
                                        .warehouse(testWarehouse)
                                        .location(testLocation)
                                        .quantity(BigDecimal.valueOf(4))
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(1)).thenReturn(Optional.of(testLocation));
                        when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
                        when(inventoryBalanceRepository.findByLotAndWarehouseAndLocationWithLock(testLot, testWarehouse, testLocation))
                                        .thenReturn(Optional.of(currentBalance));

                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
                        verify(stockMovementRepository, never()).save(any());
                }
        }

        // =========================================================================
        // GET ON-HAND QUANTITY TESTS
        // =========================================================================

        @Nested
        @DisplayName("getOnHandQuantity() Tests")
        class GetOnHandQuantityTests {

                @Test
                @DisplayName("Happy Path: Returns on-hand quantity")
                void getOnHandQuantity_WithValidParams_ReturnsQuantity() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockMovementRepository.calculateOnHandQuantity(testLot, testWarehouse, null))
                                        .thenReturn(BigDecimal.valueOf(75));

                        // Act
                        BigDecimal result = inventoryService.getOnHandQuantity(1, 1, null);

                        // Assert
                        assertEquals(BigDecimal.valueOf(75), result);
                }

                @Test
                @DisplayName("Happy Path: Returns on-hand quantity with specific location")
                void getOnHandQuantity_WithLocation_ReturnsQuantity() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(1)).thenReturn(Optional.of(testLocation));
                        when(stockMovementRepository.calculateOnHandQuantity(testLot, testWarehouse, testLocation))
                                        .thenReturn(BigDecimal.valueOf(25));

                        // Act
                        BigDecimal result = inventoryService.getOnHandQuantity(1, 1, 1);

                        // Assert
                        assertEquals(BigDecimal.valueOf(25), result);
                }

                @Test
                @DisplayName("Negative Case: Throws WAREHOUSE_NOT_FOUND")
                void getOnHandQuantity_WhenWarehouseNotFound_ThrowsException() {
                        // Arrange
                        when(warehouseRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.getOnHandQuantity(1, 999, null));

                        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws SUPPLY_LOT_NOT_FOUND")
                void getOnHandQuantity_WhenLotNotFound_ThrowsException() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.getOnHandQuantity(999, 1, null));

                        assertEquals(ErrorCode.SUPPLY_LOT_NOT_FOUND, exception.getErrorCode());
                }

                @Test
                @DisplayName("Negative Case: Throws LOCATION_NOT_FOUND")
                void getOnHandQuantity_WhenLocationNotFound_ThrowsException() {
                        // Arrange
                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(stockLocationRepository.findById(999)).thenReturn(Optional.empty());

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.getOnHandQuantity(1, 1, 999));

                        assertEquals(ErrorCode.LOCATION_NOT_FOUND, exception.getErrorCode());
                }
        }

        // =========================================================================
        // WAREHOUSE OWNERSHIP TESTS
        // =========================================================================

        @Nested
        @DisplayName("Warehouse Ownership Tests")
        class WarehouseOwnershipTests {

                @Test
                @DisplayName("Throws FORBIDDEN when warehouse has no farm")
                void ensureWarehouseOwnership_WhenNoFarm_ThrowsForbidden() {
                        // Arrange
                        Warehouse orphanWarehouse = Warehouse.builder()
                                        .id(99)
                                        .name("Orphan")
                                        .farm(null) // No farm
                                        .build();

                        when(warehouseRepository.findById(99)).thenReturn(Optional.of(orphanWarehouse));

                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(99)
                                        .supplyLotId(1)
                                        .movementType("IN")
                                        .quantity(BigDecimal.valueOf(10))
                                        .build();

                        // Act & Assert
                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
                }
        }

        @Nested
        @DisplayName("Supply Lot Ownership Tests")
        class SupplyLotOwnershipTests {

                @Test
                @DisplayName("Throws FORBIDDEN when adjusting a lot outside accessible farms")
                void recordMovement_WhenSupplyLotNotOwned_ThrowsForbidden() {
                        RecordStockMovementRequest request = RecordStockMovementRequest.builder()
                                        .warehouseId(1)
                                        .supplyLotId(1)
                                        .movementType("ADJUST")
                                        .quantity(BigDecimal.valueOf(5))
                                        .note("Cycle count")
                                        .build();

                        when(warehouseRepository.findById(1)).thenReturn(Optional.of(testWarehouse));
                        doNothing().when(farmAccessService).assertCurrentUserCanAccessFarm(testFarm);
                        when(supplyLotRepository.findById(1)).thenReturn(Optional.of(testLot));
                        when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));
                        when(inventoryBalanceRepository.existsBySupplyLot_IdAndWarehouse_Farm_IdIn(1, List.of(1)))
                                        .thenReturn(false);
                        when(inventoryBalanceRepository.existsBySupplyLot_Id(1)).thenReturn(true);

                        AppException exception = assertThrows(AppException.class,
                                        () -> inventoryService.recordMovement(request));

                        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
                        verify(stockMovementRepository, never()).save(any());
                }
        }
}
