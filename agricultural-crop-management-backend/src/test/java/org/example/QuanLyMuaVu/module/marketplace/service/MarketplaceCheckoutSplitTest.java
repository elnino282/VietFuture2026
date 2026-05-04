package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateOrderRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCreateOrderResultResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.*;
import org.example.QuanLyMuaVu.module.marketplace.model.*;
import org.example.QuanLyMuaVu.module.marketplace.repository.*;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for multi-seller checkout splitting and COD auto-advance.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceCheckoutSplitTest {

    @Mock private MarketplaceProductRepository marketplaceProductRepository;
    @Mock private MarketplaceCartRepository marketplaceCartRepository;
    @Mock private MarketplaceCartItemRepository marketplaceCartItemRepository;
    @Mock private MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;
    @Mock private MarketplaceOrderRepository marketplaceOrderRepository;
    @Mock private MarketplaceAddressRepository marketplaceAddressRepository;
    @Mock private MarketplaceProductReviewRepository marketplaceProductReviewRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private ProductWarehouseLotRepository productWarehouseLotRepository;
    @Mock private ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private ObjectMapper objectMapper;
    @Mock private AppProperties appProperties;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @InjectMocks private MarketplaceService marketplaceService;

    private User buyer;
    private MarketplaceCart cart;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(10L).username("buyer").fullName("Buyer").build();
        cart = MarketplaceCart.builder().id(100L).user(buyer).build();
        lenient().when(marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(anyLong(), anyLong()))
                .thenReturn(List.of());
        lenient().when(marketplaceProductRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(productWarehouseTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(productWarehouseLotRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("3 items from 2 sellers → 2 child orders")
    void checkout_ThreeItemsTwoSellers_SplitsIntoTwoOrders() throws Exception {
        ProductWarehouseLot lotA = buildLot(1, "50", 20L);
        ProductWarehouseLot lotB = buildLot(2, "30", 21L);
        MarketplaceProduct pA1 = buildProduct(200L, lotA, 20L, new BigDecimal("100000"));
        MarketplaceProduct pA2 = buildProduct(201L, lotA, 20L, new BigDecimal("50000"));
        MarketplaceProduct pB1 = buildProduct(202L, lotB, 21L, new BigDecimal("80000"));

        MarketplaceCartItem i1 = buildCartItem(1L, pA1, "2");
        MarketplaceCartItem i2 = buildCartItem(2L, pA2, "1");
        MarketplaceCartItem i3 = buildCartItem(3L, pB1, "3");

        MarketplaceOrderGroup group = MarketplaceOrderGroup.builder()
                .id(99L).groupCode("MOG-SPLIT").buyerUser(buyer)
                .idempotencyKey("key").requestFingerprint("fp").build();

        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of(i1, i2, i3));
        when(marketplaceOrderGroupRepository.findByBuyerUser_IdAndIdempotencyKey(10L, "key")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(marketplaceProductRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(pA1, pA2, pB1));
        when(productWarehouseLotRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(lotA, lotB));
        when(marketplaceOrderGroupRepository.save(any())).thenReturn(group);
        when(marketplaceOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                MarketplacePaymentMethod.BANK_TRANSFER, null, "Name", "0909", "Addr", null, "key");

        MarketplaceCreateOrderResultResponse response = marketplaceService.createOrder(request, null);

        assertNotNull(response);
        assertEquals(2, response.splitCount(), "Should split into 2 child orders");
        assertEquals("MOG-SPLIT", response.orderGroupCode());
    }

    @Test
    @DisplayName("COD orders auto-advance to PAYMENT_VERIFIED")
    void checkout_COD_AutoAdvancesToPaymentVerified() throws Exception {
        ProductWarehouseLot lot = buildLot(1, "50", 20L);
        MarketplaceProduct product = buildProduct(200L, lot, 20L, new BigDecimal("100000"));
        MarketplaceCartItem item = buildCartItem(1L, product, "2");

        MarketplaceOrderGroup group = MarketplaceOrderGroup.builder()
                .id(99L).groupCode("MOG-COD").buyerUser(buyer)
                .idempotencyKey("cod-key").requestFingerprint("fp").build();

        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of(item));
        when(marketplaceOrderGroupRepository.findByBuyerUser_IdAndIdempotencyKey(10L, "cod-key")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(marketplaceProductRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(product));
        when(productWarehouseLotRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(lot));
        when(marketplaceOrderGroupRepository.save(any())).thenReturn(group);
        when(marketplaceOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                MarketplacePaymentMethod.COD, null, "Name", "0909", "Addr", null, "cod-key");

        MarketplaceCreateOrderResultResponse response = marketplaceService.createOrder(request, null);

        assertNotNull(response);
        assertEquals(1, response.splitCount());
        assertEquals(MarketplaceOrderStatus.PAYMENT_VERIFIED, response.orders().getFirst().status());
    }

    @Test
    @DisplayName("Insufficient stock blocks checkout entirely")
    void checkout_InsufficientStock_ThrowsConflict() throws Exception {
        ProductWarehouseLot lot = buildLot(1, "1", 20L);
        MarketplaceProduct product = buildProduct(200L, lot, 20L, new BigDecimal("100000"));
        MarketplaceCartItem item = buildCartItem(1L, product, "5");

        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of(item));
        when(marketplaceOrderGroupRepository.findByBuyerUser_IdAndIdempotencyKey(10L, "fail-key")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(marketplaceProductRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(product));
        when(productWarehouseLotRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(lot));

        MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                MarketplacePaymentMethod.COD, null, "Name", "0909", "Addr", null, "fail-key");

        AppException ex = assertThrows(AppException.class, () -> marketplaceService.createOrder(request, null));
        assertEquals(ErrorCode.MARKETPLACE_STOCK_CONFLICT, ex.getErrorCode());
    }

    private ProductWarehouseLot buildLot(int id, String onHand, Long farmerId) {
        User farmer = User.builder().id(farmerId).username("farmer-" + farmerId).build();
        Farm farm = Farm.builder().id(id).name("Farm " + id).user(farmer).build();
        Plot plot = Plot.builder().id(id).farm(farm).plotName("Plot").build();
        Season season = Season.builder().id(id).seasonName("Season").plot(plot).build();
        return ProductWarehouseLot.builder()
                .id(id).lotCode("LOT-" + id).productName("P").farm(farm).season(season).plot(plot)
                .unit("kg").initialQuantity(new BigDecimal(onHand)).onHandQuantity(new BigDecimal(onHand))
                .status(ProductWarehouseLotStatus.IN_STOCK).build();
    }

    private MarketplaceProduct buildProduct(Long id, ProductWarehouseLot lot, Long farmerId, BigDecimal price) {
        return MarketplaceProduct.builder()
                .id(id).slug("p-" + id).name("Product " + id).price(price).unit("kg")
                .stockQuantity(lot.getOnHandQuantity()).status(MarketplaceProductStatus.PUBLISHED)
                .traceable(false).lot(lot).farm(lot.getFarm()).season(lot.getSeason())
                .farmerUser(User.builder().id(farmerId).username("f-" + farmerId).build()).build();
    }

    private MarketplaceCartItem buildCartItem(Long id, MarketplaceProduct product, String qty) {
        return MarketplaceCartItem.builder()
                .id(id).cart(cart).product(product).quantity(new BigDecimal(qty))
                .unitPriceSnapshot(product.getPrice()).build();
    }
}
