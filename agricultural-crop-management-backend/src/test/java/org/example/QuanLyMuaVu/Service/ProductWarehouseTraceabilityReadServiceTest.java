package org.example.QuanLyMuaVu.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.example.QuanLyMuaVu.module.inventory.port.ProductWarehouseTraceabilitySummaryView;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.inventory.service.ProductWarehouseTraceabilityReadService;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWarehouseTraceabilityReadServiceTest {

    @Mock
    ProductWarehouseLotRepository productWarehouseLotRepository;
    @Mock
    ProductWarehouseTransactionRepository productWarehouseTransactionRepository;

    @InjectMocks
    ProductWarehouseTraceabilityReadService productWarehouseTraceabilityReadService;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("findTraceabilitySummaryByLotId maps farm/plot/season/harvest/lot and milestones")
    void findTraceabilitySummaryByLotId_mapsSourceChainAndMilestones() throws JsonProcessingException {
        Farm farm = Farm.builder().id(1).name("Farm Alpha").build();
        Plot plot = Plot.builder().id(2).plotName("Plot A").farm(farm).build();
        Season season = Season.builder()
                .id(3)
                .seasonName("Spring 2026")
                .plot(plot)
                .startDate(LocalDate.of(2026, 1, 10))
                .plannedHarvestDate(LocalDate.of(2026, 3, 30))
                .build();
        Harvest harvest = Harvest.builder()
                .id(4)
                .season(season)
                .harvestDate(LocalDate.of(2026, 3, 11))
                .quantity(BigDecimal.valueOf(120))
                .grade("A")
                .note("internal note should not leak")
                .build();
        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(99)
                .lotCode("LOT-99")
                .farm(farm)
                .plot(plot)
                .season(season)
                .harvest(harvest)
                .productName("Rice")
                .productVariant("Jasmine")
                .harvestedAt(LocalDate.of(2026, 3, 11))
                .receivedAt(LocalDateTime.of(2026, 3, 12, 9, 0))
                .initialQuantity(BigDecimal.valueOf(120))
                .onHandQuantity(BigDecimal.valueOf(85))
                .unit("kg")
                .traceabilityData("{\"secret\":\"hidden\"}")
                .note("internal lot note")
                .build();

        ProductWarehouseTransaction receipt = ProductWarehouseTransaction.builder()
                .id(10)
                .lot(lot)
                .transactionType(ProductWarehouseTransactionType.RECEIPT_FROM_HARVEST)
                .quantity(BigDecimal.valueOf(120))
                .unit("kg")
                .resultingOnHand(BigDecimal.valueOf(120))
                .referenceType("HARVEST")
                .referenceId("4")
                .note("internal")
                .createdAt(LocalDateTime.of(2026, 3, 12, 9, 0))
                .build();
        ProductWarehouseTransaction stockOut = ProductWarehouseTransaction.builder()
                .id(11)
                .lot(lot)
                .transactionType(ProductWarehouseTransactionType.STOCK_OUT)
                .quantity(BigDecimal.valueOf(35))
                .unit("kg")
                .resultingOnHand(BigDecimal.valueOf(85))
                .referenceType("ORDER")
                .referenceId("ord-1")
                .note("internal")
                .createdAt(LocalDateTime.of(2026, 3, 13, 11, 0))
                .build();

        when(productWarehouseLotRepository.findById(99)).thenReturn(Optional.of(lot));
        when(productWarehouseTransactionRepository.findAllByLot_IdOrderByCreatedAtDesc(99))
                .thenReturn(List.of(stockOut, receipt));

        ProductWarehouseTraceabilitySummaryView summary = productWarehouseTraceabilityReadService
                .findTraceabilitySummaryByLotId(99)
                .orElseThrow();

        assertEquals(99, summary.lotId());
        assertEquals("LOT-99", summary.lotCode());
        assertEquals(1, summary.farm().id());
        assertEquals("Farm Alpha", summary.farm().name());
        assertEquals(2, summary.plot().id());
        assertEquals("Plot A", summary.plot().name());
        assertEquals(3, summary.season().id());
        assertEquals("Spring 2026", summary.season().name());
        assertEquals(LocalDate.of(2026, 1, 10), summary.season().startDate());
        assertEquals(LocalDate.of(2026, 3, 30), summary.season().plannedHarvestDate());
        assertEquals(4, summary.harvest().id());
        assertEquals(LocalDate.of(2026, 3, 11), summary.harvest().harvestedAt());
        assertEquals(BigDecimal.valueOf(120), summary.harvest().quantity());
        assertEquals("A", summary.harvest().grade());
        assertEquals("Rice", summary.lot().productName());
        assertEquals("Jasmine", summary.lot().productVariant());
        assertEquals(BigDecimal.valueOf(120), summary.lot().initialQuantity());

        assertNotNull(summary.milestones());
        assertEquals(2, summary.milestones().size());
        assertEquals(ProductWarehouseTransactionType.RECEIPT_FROM_HARVEST.name(), summary.milestones().get(0).transactionType());
        assertEquals("IN", summary.milestones().get(0).movementType());
        assertEquals(ProductWarehouseTransactionType.STOCK_OUT.name(), summary.milestones().get(1).transactionType());
        assertEquals("OUT", summary.milestones().get(1).movementType());

        String summaryJson = objectMapper.writeValueAsString(summary);
        assertFalse(summaryJson.contains("traceabilityData"));
        assertFalse(summaryJson.contains("referenceType"));
        assertFalse(summaryJson.contains("referenceId"));
        assertFalse(summaryJson.contains("note"));
        assertFalse(summaryJson.contains("createdBy"));
    }

    @Test
    @DisplayName("findTraceabilitySummaryByLotId returns empty when lot id is null")
    void findTraceabilitySummaryByLotId_whenLotIdIsNull_returnsEmpty() {
        Optional<ProductWarehouseTraceabilitySummaryView> summary =
                productWarehouseTraceabilityReadService.findTraceabilitySummaryByLotId(null);
        assertTrue(summary.isEmpty());
    }

    @Test
    @DisplayName("findTraceabilitySummaryByLotId returns empty when lot is not found")
    void findTraceabilitySummaryByLotId_whenLotNotFound_returnsEmpty() {
        when(productWarehouseLotRepository.findById(1234)).thenReturn(Optional.empty());

        Optional<ProductWarehouseTraceabilitySummaryView> summary =
                productWarehouseTraceabilityReadService.findTraceabilitySummaryByLotId(1234);
        assertTrue(summary.isEmpty());
    }
}
