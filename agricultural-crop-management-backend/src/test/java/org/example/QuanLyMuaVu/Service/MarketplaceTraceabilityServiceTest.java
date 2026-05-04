package org.example.QuanLyMuaVu.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceTraceabilityResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceService;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceTraceabilityServiceTest {

    @Mock MarketplaceProductRepository marketplaceProductRepository;
    @Mock MarketplaceOrderRepository marketplaceOrderRepository;
    @Mock CurrentUserService currentUserService;
    // These are required by MarketplaceService constructor but unused in traceability tests
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartRepository marketplaceCartRepository;
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartItemRepository marketplaceCartItemRepository;
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderItemRepository marketplaceOrderItemRepository;
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceAddressRepository marketplaceAddressRepository;
    @Mock org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductReviewRepository marketplaceProductReviewRepository;
    @Mock org.example.QuanLyMuaVu.module.farm.repository.FarmRepository farmRepository;
    @Mock org.example.QuanLyMuaVu.module.season.repository.SeasonRepository seasonRepository;
    @Mock org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository productWarehouseLotRepository;
    @Mock org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock AppProperties appProperties;
    @Mock org.example.QuanLyMuaVu.module.admin.service.AuditLogService auditLogService;
    @Mock org.example.QuanLyMuaVu.module.incident.service.NotificationService notificationService;

    @InjectMocks MarketplaceService marketplaceService;

    ObjectMapper realMapper = new ObjectMapper().findAndRegisterModules();

    // ─────────── Test fixtures ───────────

    private Farm buildFarm() {
        Province province = Province.builder().id(1).name("Đồng Tháp").nameWithType("Tỉnh Đồng Tháp").build();
        Ward ward = Ward.builder().id(100).name("Tân Hồng").build();
        return Farm.builder()
                .id(10)
                .name("Trang Trại Lúa Vàng")
                .province(province)
                .ward(ward)
                .area(BigDecimal.valueOf(50))
                .active(true)
                .build();
    }

    private Plot buildPlot(Farm farm) {
        return Plot.builder()
                .id(20)
                .plotName("Thửa A1")
                .area(BigDecimal.valueOf(12.5))
                .farm(farm)
                .status("IN_USE")
                .build();
    }

    private Season buildSeason(Plot plot) {
        Crop crop = Crop.builder().id(1).cropName("Lúa").build();
        Variety variety = Variety.builder().id(1).name("Jasmine 85").crop(crop).build();
        return Season.builder()
                .id(30)
                .seasonName("Vụ Đông Xuân 2026")
                .plot(plot)
                .crop(crop)
                .variety(variety)
                .startDate(LocalDate.of(2026, 1, 15))
                .plannedHarvestDate(LocalDate.of(2026, 4, 20))
                .status(SeasonStatus.COMPLETED)
                .initialPlantCount(1000)
                .build();
    }

    private Harvest buildHarvest(Season season) {
        return Harvest.builder()
                .id(40)
                .season(season)
                .harvestDate(LocalDate.of(2026, 4, 18))
                .quantity(BigDecimal.valueOf(5000))
                .grade("A")
                .note("Internal note — should NOT appear in response")
                .build();
    }

    private ProductWarehouseLot buildLot(Farm farm, Plot plot, Season season, Harvest harvest) {
        Warehouse warehouse = Warehouse.builder()
                .id(1).name("Kho Chính").farm(farm).build();
        StockLocation location = StockLocation.builder()
                .id(1).warehouse(warehouse).zone("A").aisle("3").shelf("2").bin("B1").build();
        return ProductWarehouseLot.builder()
                .id(50)
                .lotCode("LOT-2026-001")
                .farm(farm)
                .plot(plot)
                .season(season)
                .harvest(harvest)
                .warehouse(warehouse)
                .location(location)
                .productName("Lúa Jasmine")
                .productVariant("Jasmine 85")
                .harvestedAt(LocalDate.of(2026, 4, 18))
                .receivedAt(LocalDateTime.of(2026, 4, 19, 10, 0))
                .initialQuantity(BigDecimal.valueOf(5000))
                .onHandQuantity(BigDecimal.valueOf(3000))
                .unit("kg")
                .grade("A")
                .qualityStatus("PASSED")
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .build();
    }

    private MarketplaceProduct buildProduct(Farm farm, Season season, ProductWarehouseLot lot) {
        User farmer = User.builder().id(99L).username("farmer01").build();
        return MarketplaceProduct.builder()
                .id(1L)
                .slug("lua-jasmine-85")
                .name("Lúa Jasmine 85")
                .price(BigDecimal.valueOf(25000))
                .unit("kg")
                .stockQuantity(BigDecimal.valueOf(1000))
                .farmerUser(farmer)
                .farm(farm)
                .season(season)
                .lot(lot)
                .traceable(true)
                .status(MarketplaceProductStatus.PUBLISHED)
                .publishedAt(LocalDateTime.of(2026, 4, 25, 14, 0))
                .build();
    }

    // ─────────── Public traceability tests ───────────

    @Nested
    @DisplayName("GET /marketplace/products/{id}/traceability")
    class PublicTraceability {

        @Test
        @DisplayName("Full traceability chain returns all 5 levels and timeline milestones")
        void fullChain_returnsComplete() throws JsonProcessingException {
            Farm farm = buildFarm();
            Plot plot = buildPlot(farm);
            Season season = buildSeason(plot);
            Harvest harvest = buildHarvest(season);
            ProductWarehouseLot lot = buildLot(farm, plot, season, harvest);
            MarketplaceProduct product = buildProduct(farm, season, lot);

            when(marketplaceProductRepository.findSellableByIdAndStatus(1L, MarketplaceProductStatus.PUBLISHED))
                    .thenReturn(Optional.of(product));

            MarketplaceTraceabilityResponse response = marketplaceService.getTraceability(1L);

            // Basic fields
            assertEquals(1L, response.productId());
            assertTrue(response.traceable());
            assertNotNull(response.validatedAt());

            // Farm
            assertNotNull(response.farm());
            assertEquals(10, response.farm().id());
            assertEquals("Trang Trại Lúa Vàng", response.farm().name());
            assertEquals("Đồng Tháp", response.farm().region());
            assertNotNull(response.farm().address());
            assertNull(response.farm().certificationInfo()); // TODO field

            // Plot
            assertNotNull(response.plot());
            assertEquals(20, response.plot().id());
            assertEquals("Thửa A1", response.plot().name());
            assertEquals(BigDecimal.valueOf(12.5), response.plot().area());

            // Season
            assertNotNull(response.season());
            assertEquals(30, response.season().id());
            assertEquals("Lúa", response.season().cropName());
            assertEquals("Jasmine 85", response.season().varietyName());
            assertEquals(LocalDate.of(2026, 1, 15), response.season().plantingDate());
            assertEquals(LocalDate.of(2026, 4, 20), response.season().harvestDate());

            // Harvest
            assertNotNull(response.harvest());
            assertEquals(40, response.harvest().id());
            assertEquals(LocalDate.of(2026, 4, 18), response.harvest().harvestDate());
            assertEquals(BigDecimal.valueOf(5000), response.harvest().quantity());
            assertEquals("A", response.harvest().qualityNotes());

            // Product lot
            assertNotNull(response.productLot());
            assertEquals(50, response.productLot().id());
            assertEquals("LOT-2026-001", response.productLot().lotCode());
            assertEquals("Kho Chính", response.productLot().warehouseName());
            assertEquals("Zone A, Aisle 3, Shelf 2, Bin B1", response.productLot().storageLocation());

            // Timeline
            assertNotNull(response.timeline());
            assertEquals(5, response.timeline().size());
            assertEquals("PLANTED", response.timeline().get(0).milestone());
            assertEquals("TENDED", response.timeline().get(1).milestone());
            assertEquals("HARVESTED", response.timeline().get(2).milestone());
            assertEquals("STORED", response.timeline().get(3).milestone());
            assertEquals("LISTED", response.timeline().get(4).milestone());

            // Security: sensitive data never exposed
            String json = realMapper.writeValueAsString(response);
            assertFalse(json.contains("Internal note"));
            assertFalse(json.contains("traceabilityData"));
            assertFalse(json.contains("createdBy"));
            assertFalse(json.contains("budgetAmount"));
        }

        @Test
        @DisplayName("Partial fallback — missing season/harvest returns partial response without error")
        void partialFallback_missingSeasonAndHarvest() {
            Farm farm = buildFarm();
            // Lot with no season, no harvest, no plot
            ProductWarehouseLot lot = ProductWarehouseLot.builder()
                    .id(50)
                    .lotCode("LOT-ORPHAN")
                    .farm(farm)
                    .warehouse(Warehouse.builder().id(1).name("Kho").farm(farm).build())
                    .harvestedAt(LocalDate.of(2026, 3, 1))
                    .receivedAt(LocalDateTime.of(2026, 3, 2, 8, 0))
                    .initialQuantity(BigDecimal.valueOf(200))
                    .onHandQuantity(BigDecimal.valueOf(200))
                    .unit("kg")
                    .status(ProductWarehouseLotStatus.IN_STOCK)
                    .build();
            User farmer = User.builder().id(99L).username("farmer01").build();
            MarketplaceProduct product = MarketplaceProduct.builder()
                    .id(2L)
                    .slug("partial-product")
                    .name("Partial Product")
                    .price(BigDecimal.TEN)
                    .unit("kg")
                    .stockQuantity(BigDecimal.valueOf(100))
                    .farmerUser(farmer)
                    .farm(farm)
                    .season(null)  // no season
                    .lot(lot)
                    .traceable(true)
                    .status(MarketplaceProductStatus.PUBLISHED)
                    .publishedAt(LocalDateTime.of(2026, 3, 5, 10, 0))
                    .build();

            when(marketplaceProductRepository.findSellableByIdAndStatus(2L, MarketplaceProductStatus.PUBLISHED))
                    .thenReturn(Optional.of(product));

            // Should NOT throw — graceful fallback
            MarketplaceTraceabilityResponse response = marketplaceService.getTraceability(2L);

            assertNotNull(response);
            assertTrue(response.traceable());
            assertNotNull(response.farm()); // farm is present
            assertNull(response.plot());    // no plot
            assertNull(response.season());  // no season
            assertNull(response.harvest()); // no harvest
            assertNotNull(response.productLot()); // lot is present

            // Timeline should still have HARVESTED (from lot.harvestedAt), STORED, LISTED
            assertEquals(3, response.timeline().size());
            assertEquals("HARVESTED", response.timeline().get(0).milestone());
            assertEquals("STORED", response.timeline().get(1).milestone());
            assertEquals("LISTED", response.timeline().get(2).milestone());
        }

        @Test
        @DisplayName("Non-traceable product returns response with traceable=false and available data")
        void nonTraceableProduct() {
            User farmer = User.builder().id(99L).username("farmer01").build();
            ProductWarehouseLot lot = ProductWarehouseLot.builder()
                    .id(50).lotCode("LOT-NT").farm(buildFarm())
                    .warehouse(Warehouse.builder().id(1).name("Kho").farm(buildFarm()).build())
                    .harvestedAt(LocalDate.now())
                    .receivedAt(LocalDateTime.now())
                    .initialQuantity(BigDecimal.TEN)
                    .onHandQuantity(BigDecimal.TEN)
                    .unit("kg")
                    .status(ProductWarehouseLotStatus.IN_STOCK)
                    .build();
            MarketplaceProduct product = MarketplaceProduct.builder()
                    .id(3L).slug("non-trace").name("No Trace")
                    .price(BigDecimal.TEN).unit("kg")
                    .stockQuantity(BigDecimal.TEN)
                    .farmerUser(farmer)
                    .farm(null).season(null).lot(lot)
                    .traceable(false) // non-traceable
                    .status(MarketplaceProductStatus.PUBLISHED)
                    .build();

            when(marketplaceProductRepository.findSellableByIdAndStatus(3L, MarketplaceProductStatus.PUBLISHED))
                    .thenReturn(Optional.of(product));

            MarketplaceTraceabilityResponse response = marketplaceService.getTraceability(3L);

            assertFalse(response.traceable());
            // Should still return whatever data is available (lot)
            assertNotNull(response.productLot());
        }

        @Test
        @DisplayName("Product not found throws MARKETPLACE_PRODUCT_NOT_FOUND")
        void productNotFound_throws() {
            when(marketplaceProductRepository.findSellableByIdAndStatus(999L, MarketplaceProductStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.getTraceability(999L));
            assertEquals("ERR_MARKETPLACE_PRODUCT_NOT_FOUND", ex.getErrorCode().getCode());
        }
    }

    // ─────────── Buyer order-item traceability tests ───────────

    @Nested
    @DisplayName("GET /buyer/orders/{orderId}/items/{itemId}/traceability")
    class BuyerOrderItemTraceability {

        @Test
        @DisplayName("Happy path — buyer gets traceability for own order item")
        void happyPath() {
            Long buyerUserId = 200L;
            when(currentUserService.getCurrentUserId()).thenReturn(buyerUserId);

            Farm farm = buildFarm();
            Plot plot = buildPlot(farm);
            Season season = buildSeason(plot);
            Harvest harvest = buildHarvest(season);
            ProductWarehouseLot lot = buildLot(farm, plot, season, harvest);
            MarketplaceProduct product = buildProduct(farm, season, lot);

            MarketplaceOrderItem orderItem = MarketplaceOrderItem.builder()
                    .id(500L)
                    .product(product)
                    .productNameSnapshot("Lúa Jasmine 85")
                    .productSlugSnapshot("lua-jasmine-85")
                    .unitPriceSnapshot(BigDecimal.valueOf(25000))
                    .quantity(BigDecimal.valueOf(100))
                    .lineTotal(BigDecimal.valueOf(2500000))
                    .traceableSnapshot(true)
                    .farm(farm)
                    .season(season)
                    .lot(lot)
                    .build();

            MarketplaceOrder order = MarketplaceOrder.builder()
                    .id(100L)
                    .items(new java.util.ArrayList<>(List.of(orderItem)))
                    .build();
            orderItem.setOrder(order);

            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(100L, buyerUserId))
                    .thenReturn(Optional.of(order));

            MarketplaceTraceabilityResponse response = marketplaceService.getOrderItemTraceability(100L, 500L);

            assertNotNull(response);
            assertEquals(1L, response.productId());
            assertTrue(response.traceable());
            assertNotNull(response.farm());
            assertNotNull(response.plot());
            assertNotNull(response.season());
            assertNotNull(response.harvest());
            assertNotNull(response.productLot());
            assertNotNull(response.timeline());
            assertFalse(response.timeline().isEmpty());
        }

        @Test
        @DisplayName("Security — buyer cannot access another buyer's order (returns ORDER_NOT_FOUND)")
        void wrongBuyer_getsNotFound() {
            Long attackerUserId = 300L;
            when(currentUserService.getCurrentUserId()).thenReturn(attackerUserId);

            // Simulate: this order belongs to buyer 200, not attacker 300
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(100L, attackerUserId))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.getOrderItemTraceability(100L, 500L));
            assertEquals("ERR_MARKETPLACE_ORDER_NOT_FOUND", ex.getErrorCode().getCode());
        }

        @Test
        @DisplayName("Order item not found in order throws ITEM_NOT_IN_ORDER")
        void itemNotInOrder_throws() {
            Long buyerUserId = 200L;
            when(currentUserService.getCurrentUserId()).thenReturn(buyerUserId);

            MarketplaceOrder order = MarketplaceOrder.builder()
                    .id(100L)
                    .items(new java.util.ArrayList<>()) // no items
                    .build();

            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(100L, buyerUserId))
                    .thenReturn(Optional.of(order));

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.getOrderItemTraceability(100L, 999L));
            assertEquals("ERR_MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER", ex.getErrorCode().getCode());
        }
    }
}
