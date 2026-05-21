package org.example.QuanLyMuaVu.Service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.IncidentSeverity;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.DashboardInventoryAlertsResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.incident.port.IncidentQueryPort;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryLowStockView;
import org.example.QuanLyMuaVu.module.inventory.port.InventoryQueryPort;
import org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardIncidentAlertResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardAlertsService;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAlertsServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private IncidentQueryPort incidentQueryPort;

    @Mock
    private InventoryQueryPort inventoryQueryPort;

    @Mock
    private FarmQueryPort farmQueryPort;

    @Mock
    private TaskQueryPort taskQueryPort;

    @Mock
    private SustainabilityDashboardService sustainabilityDashboardService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Inventory inventoryProperties;

    @Mock
    private AppProperties.InventoryAlerts inventoryAlertsProperties;

    @InjectMocks
    private DashboardAlertsService dashboardAlertsService;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getInventory()).thenReturn(inventoryProperties);
        lenient().when(inventoryProperties.getAlerts()).thenReturn(inventoryAlertsProperties);
        lenient().when(inventoryAlertsProperties.getLowStockThreshold()).thenReturn(BigDecimal.valueOf(5));
        lenient().when(inventoryAlertsProperties.getExpiringSoonDays()).thenReturn(30);
        lenient().when(inventoryAlertsProperties.getNoMovementDays()).thenReturn(30);
        lenient().when(inventoryAlertsProperties.getAbnormalAdjustCount()).thenReturn(2);
        lenient().when(inventoryAlertsProperties.getAbnormalAdjustRatio()).thenReturn(new BigDecimal("0.50"));
    }

    @Test
    @DisplayName("buildAlerts aggregates incident and inventory metrics")
    void buildAlerts_AggregatesMetrics() {
        when(incidentQueryPort.countOpenIncidentsByOwnerId(1L)).thenReturn(4L);
        when(inventoryQueryPort.countExpiringLotsByOwnerId(eq(1L), any(LocalDate.class))).thenReturn(3L);
        when(inventoryQueryPort.findLowStockByOwnerId(1L, 100, BigDecimal.valueOf(5)))
                .thenReturn(List.of(
                        new InventoryLowStockView(1, "LOT-1", "NPK", "Warehouse A", "", BigDecimal.ONE, "kg"),
                        new InventoryLowStockView(2, "LOT-2", "Urea", "Warehouse B", "", BigDecimal.valueOf(2), "kg")));

        DashboardOverviewResponse.Alerts alerts = dashboardAlertsService.buildAlerts(1L);

        assertNotNull(alerts);
        assertEquals(4, alerts.getOpenIncidents());
        assertEquals(3, alerts.getExpiringLots());
        assertEquals(2, alerts.getLowStockItems());
    }

    @Test
    @DisplayName("getIncidentAlerts returns merged open incidents, overdue tasks, and sustainability warnings")
    void getIncidentAlerts_ReturnsMergedOperationalAlerts() {
        User owner = User.builder().id(1L).username("farmer").build();
        Farm farm = Farm.builder().id(21).name("Farm 21").user(owner).build();
        Plot plot = Plot.builder().id(31).plotName("Plot 31").farm(farm).build();
        Season season = Season.builder().id(41).seasonName("Season 41").plot(plot).build();

        Incident highIncident = Incident.builder()
                .id(10)
                .seasonId(41)
                .season(season)
                .severity(IncidentSeverity.HIGH)
                .status(IncidentStatus.OPEN)
                .incidentType("DISEASE")
                .description("Disease spread detected")
                .createdAt(LocalDateTime.now().minusHours(2))
                .deadline(LocalDate.now().plusDays(1))
                .build();

        DashboardTaskView overdueTask = new DashboardTaskView();
        overdueTask.setTaskId(55);
        overdueTask.setTitle("Apply treatment");
        overdueTask.setSeasonId(41);
        overdueTask.setPlotId(31);
        overdueTask.setPlotName("Plot 31");
        overdueTask.setDueDate(LocalDate.now().minusDays(4));

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(incidentQueryPort.findOpenIncidentsByOwnerId(1L, 41)).thenReturn(List.of(highIncident));
        when(taskQueryPort.findOverdueTasksByUser(eq(1L), eq(41), any(LocalDate.class), any(), eq(30)))
                .thenReturn(List.of(overdueTask));
        when(sustainabilityDashboardService.getFieldMap(eq(41), isNull(), isNull(), isNull()))
                .thenReturn(FieldMapResponse.builder().fieldsWithBoundary(List.of(
                        FieldMapResponse.FieldMapItem.builder()
                                .fieldId(31)
                                .fieldName("Plot 31")
                                .fdnLevel("high")
                                .missingInputs(List.of("MINERAL_FERTILIZER"))
                                .build())).build());

        List<DashboardIncidentAlertResponse> alerts = dashboardAlertsService.getIncidentAlerts(41);

        assertEquals(3, alerts.size());
        assertTrue(alerts.stream().anyMatch(alert -> "HIGH_SEVERITY_INCIDENT".equals(alert.getType())));
        assertTrue(alerts.stream().anyMatch(alert -> "OVERDUE_TASK".equals(alert.getType())));
        assertTrue(alerts.stream().anyMatch(alert -> "SUSTAINABILITY_WARNING".equals(alert.getType())));
    }

    @Test
    @DisplayName("getLowStock maps inventory views to API response")
    void getLowStock_MapsViewToResponse() {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(inventoryQueryPort.findLowStockByOwnerId(1L, 5, BigDecimal.valueOf(5)))
                .thenReturn(List.of(
                        new InventoryLowStockView(7, "LOT-007", "Potassium", "Main WH", "", BigDecimal.valueOf(3), "kg")));

        List<LowStockAlertResponse> result = dashboardAlertsService.getLowStock(5);

        assertEquals(1, result.size());
        LowStockAlertResponse row = result.get(0);
        assertEquals(7, row.getSupplyLotId());
        assertEquals("LOT-007", row.getBatchCode());
        assertEquals("Potassium", row.getItemName());
        assertEquals("Main WH", row.getWarehouseName());
        assertEquals(BigDecimal.valueOf(3), row.getOnHand());
        assertEquals("kg", row.getUnit());
        verify(currentUserService).getCurrentUserId();
    }

    @Test
    @DisplayName("getLowStock returns empty when limit is invalid")
    void getLowStock_WithInvalidLimit_ReturnsEmpty() {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);

        List<LowStockAlertResponse> result = dashboardAlertsService.getLowStock(0);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buildSustainabilityAlerts aggregates levels and missing inputs")
    void buildSustainabilityAlerts_AggregatesFieldLevels() {
        when(sustainabilityDashboardService.getFieldMap(12, null, null, null))
                .thenReturn(FieldMapResponse.builder().fieldsWithBoundary(List.of(
                        FieldMapResponse.FieldMapItem.builder().fdnLevel("high").missingInputs(List.of("mineral")).build(),
                        FieldMapResponse.FieldMapItem.builder().fdnLevel("medium").missingInputs(List.of()).build(),
                        FieldMapResponse.FieldMapItem.builder().fdnLevel("low").missingInputs(null).build())).build());

        DashboardOverviewResponse.SustainabilityAlerts alerts = dashboardAlertsService.buildSustainabilityAlerts(12);

        assertEquals(3, alerts.getTotalFields());
        assertEquals(1, alerts.getHighRiskFields());
        assertEquals(1, alerts.getMediumRiskFields());
        assertEquals(1, alerts.getLowRiskFields());
        assertEquals(1, alerts.getFieldsMissingInputs());
    }

    @Test
    @DisplayName("getInventoryAlerts computes expired, expiring soon, low stock and no movement from real lot balances")
    void getInventoryAlerts_ComputesWarehouseRisks() {
        LocalDate today = LocalDate.now();
        Farm farm = Farm.builder().id(10).name("Farm A").build();
        Warehouse warehouse = Warehouse.builder().id(20).name("Main Warehouse").farm(farm).build();
        StockLocation location = StockLocation.builder()
                .id(30)
                .zone("Z1")
                .aisle("A1")
                .shelf("S1")
                .bin("B1")
                .build();

        SupplyItem item = SupplyItem.builder().id(100).name("NPK").unit("kg").build();

        SupplyLot expiredLot = SupplyLot.builder()
                .id(1001)
                .supplyItem(item)
                .batchCode("LOT-EX")
                .expiryDate(today.minusDays(1))
                .build();
        SupplyLot expiringLot = SupplyLot.builder()
                .id(1002)
                .supplyItem(item)
                .batchCode("LOT-SOON")
                .expiryDate(today.plusDays(5))
                .build();
        SupplyLot lowStockLot = SupplyLot.builder()
                .id(1003)
                .supplyItem(item)
                .batchCode("LOT-LOW")
                .expiryDate(today.plusDays(90))
                .build();
        SupplyLot noMovementLot = SupplyLot.builder()
                .id(1004)
                .supplyItem(item)
                .batchCode("LOT-IDLE")
                .expiryDate(today.plusDays(90))
                .build();

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(farmQueryPort.findFarmsByOwnerId(1L)).thenReturn(List.of(farm));
        when(inventoryQueryPort.findAllInventoryBalancesWithDetails()).thenReturn(List.of(
                balance(expiredLot, warehouse, location, "8"),
                balance(expiringLot, warehouse, location, "12"),
                balance(lowStockLot, warehouse, location, "2"),
                balance(noMovementLot, warehouse, location, "14")));

        when(inventoryQueryPort.findStockMovementsBySupplyLotId(1001)).thenReturn(List.of(
                movement(expiredLot, warehouse, LocalDateTime.now().minusDays(2), StockMovementType.OUT, "1")));
        when(inventoryQueryPort.findStockMovementsBySupplyLotId(1002)).thenReturn(List.of(
                movement(expiringLot, warehouse, LocalDateTime.now().minusDays(3), StockMovementType.OUT, "2")));
        when(inventoryQueryPort.findStockMovementsBySupplyLotId(1003)).thenReturn(List.of(
                movement(lowStockLot, warehouse, LocalDateTime.now().minusDays(1), StockMovementType.OUT, "1")));
        when(inventoryQueryPort.findStockMovementsBySupplyLotId(1004)).thenReturn(List.of(
                movement(noMovementLot, warehouse, LocalDateTime.now().minusDays(45), StockMovementType.OUT, "1")));

        DashboardInventoryAlertsResponse response = dashboardAlertsService.getInventoryAlerts(20);

        assertNotNull(response);
        assertNotNull(response.getSummary());
        assertEquals(4, response.getSummary().getTotalAlerts());
        assertEquals(1, response.getSummary().getExpired());
        assertEquals(1, response.getSummary().getExpiringSoon());
        assertEquals(1, response.getSummary().getLowStock());
        assertEquals(1, response.getSummary().getNoMovement());
        assertEquals(0, response.getSummary().getAbnormalMovement());
        assertEquals(4, response.getAlerts().size());
        assertTrue(response.getAlerts().stream().anyMatch(alert -> "EXPIRED".equals(alert.getAlertType())));
        assertTrue(response.getAlerts().stream().anyMatch(alert -> "EXPIRING_SOON".equals(alert.getAlertType())));
        assertTrue(response.getAlerts().stream().anyMatch(alert -> "LOW_STOCK".equals(alert.getAlertType())));
        assertTrue(response.getAlerts().stream().anyMatch(alert -> "NO_MOVEMENT".equals(alert.getAlertType())));
    }

    private InventoryBalance balance(SupplyLot lot, Warehouse warehouse, StockLocation location, String quantity) {
        return InventoryBalance.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .location(location)
                .quantity(new BigDecimal(quantity))
                .build();
    }

    private StockMovement movement(
            SupplyLot lot,
            Warehouse warehouse,
            LocalDateTime movementDate,
            StockMovementType movementType,
            String quantity) {
        return StockMovement.builder()
                .supplyLot(lot)
                .warehouse(warehouse)
                .movementDate(movementDate)
                .movementType(movementType)
                .quantity(new BigDecimal(quantity))
                .build();
    }
}
