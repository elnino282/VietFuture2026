package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateOrderStatusRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderGroup;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.*;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketplaceOrderStatusLifecycleTest {

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

    private User farmer;
    private User buyer;

    @BeforeEach
    void setUp() {
        farmer = User.builder().id(20L).username("farmer-20").fullName("Farmer 20").build();
        buyer = User.builder().id(10L).username("buyer-10").fullName("Buyer 10").build();
        lenient().when(marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(anyLong(), anyLong()))
                .thenReturn(List.of());
        lenient().when(marketplaceProductRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(productWarehouseTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(productWarehouseLotRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Happy path lifecycle")
    class HappyPath {
        @Test
        @DisplayName("PAYMENT_VERIFIED → CONFIRMED → PREPARING → SHIPPED → DELIVERED → COMPLETED")
        void fullLifecycle() {
            MarketplaceOrder order = buildOrder(MarketplaceOrderStatus.PAYMENT_VERIFIED);
            assertTransition(order, MarketplaceOrderStatus.CONFIRMED);
            assertTransition(order, MarketplaceOrderStatus.PREPARING);
            assertTransition(order, MarketplaceOrderStatus.SHIPPED);
            assertTransition(order, MarketplaceOrderStatus.DELIVERED);
            assertTransition(order, MarketplaceOrderStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {
        static Stream<Arguments> cases() {
            return Stream.of(
                Arguments.of(MarketplaceOrderStatus.PENDING_PAYMENT, MarketplaceOrderStatus.CONFIRMED),
                Arguments.of(MarketplaceOrderStatus.CONFIRMED, MarketplaceOrderStatus.SHIPPED),
                Arguments.of(MarketplaceOrderStatus.PREPARING, MarketplaceOrderStatus.DELIVERED),
                Arguments.of(MarketplaceOrderStatus.SHIPPED, MarketplaceOrderStatus.COMPLETED),
                Arguments.of(MarketplaceOrderStatus.CONFIRMED, MarketplaceOrderStatus.PAYMENT_VERIFIED),
                Arguments.of(MarketplaceOrderStatus.COMPLETED, MarketplaceOrderStatus.CANCELLED),
                Arguments.of(MarketplaceOrderStatus.CANCELLED, MarketplaceOrderStatus.CONFIRMED),
                Arguments.of(MarketplaceOrderStatus.REJECTED, MarketplaceOrderStatus.CONFIRMED)
            );
        }

        @ParameterizedTest(name = "{0} → {1} should fail")
        @MethodSource("cases")
        void invalidTransition_ThrowsBadRequest(MarketplaceOrderStatus from, MarketplaceOrderStatus to) {
            when(currentUserService.getCurrentUserId()).thenReturn(20L);
            MarketplaceOrder order = buildOrder(from);
            when(marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(1L, 20L)).thenReturn(Optional.of(order));
            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.updateFarmerOrderStatus(1L, new MarketplaceUpdateOrderStatusRequest(to)));
            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("REJECTED status")
    class RejectedTests {
        @Test
        void farmerCanRejectPendingPayment() {
            MarketplaceOrder order = buildOrder(MarketplaceOrderStatus.PENDING_PAYMENT);
            setupRejectMocks(order);
            MarketplaceOrderResponse r = marketplaceService.updateFarmerOrderStatus(1L,
                    new MarketplaceUpdateOrderStatusRequest(MarketplaceOrderStatus.REJECTED));
            assertEquals(MarketplaceOrderStatus.REJECTED, r.status());
        }

        @Test
        void farmerCanRejectPaymentSubmitted() {
            MarketplaceOrder order = buildOrder(MarketplaceOrderStatus.PAYMENT_SUBMITTED);
            setupRejectMocks(order);
            MarketplaceOrderResponse r = marketplaceService.updateFarmerOrderStatus(1L,
                    new MarketplaceUpdateOrderStatusRequest(MarketplaceOrderStatus.REJECTED));
            assertEquals(MarketplaceOrderStatus.REJECTED, r.status());
        }

        @Test
        void cannotRejectConfirmedOrder() {
            when(currentUserService.getCurrentUserId()).thenReturn(20L);
            MarketplaceOrder order = buildOrder(MarketplaceOrderStatus.CONFIRMED);
            when(marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(1L, 20L)).thenReturn(Optional.of(order));
            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.updateFarmerOrderStatus(1L,
                            new MarketplaceUpdateOrderStatusRequest(MarketplaceOrderStatus.REJECTED)));
            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        }

        private void setupRejectMocks(MarketplaceOrder order) {
            when(currentUserService.getCurrentUserId()).thenReturn(20L);
            when(currentUserService.getCurrentUser()).thenReturn(farmer);
            when(marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(1L, 20L)).thenReturn(Optional.of(order));
            when(marketplaceOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(marketplaceProductRepository.findAllByIdInForUpdate(any())).thenReturn(List.of());
            when(productWarehouseLotRepository.findAllByIdInForUpdate(any())).thenReturn(List.of());
        }
    }

    private void assertTransition(MarketplaceOrder order, MarketplaceOrderStatus target) {
        when(currentUserService.getCurrentUserId()).thenReturn(20L);
        when(marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(1L, 20L)).thenReturn(Optional.of(order));
        when(marketplaceOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        MarketplaceOrderResponse r = marketplaceService.updateFarmerOrderStatus(1L,
                new MarketplaceUpdateOrderStatusRequest(target));
        assertNotNull(r);
        assertEquals(target, r.status());
    }

    private MarketplaceOrder buildOrder(MarketplaceOrderStatus status) {
        MarketplaceOrderGroup group = MarketplaceOrderGroup.builder().id(1L).groupCode("MOG-TEST").build();
        return MarketplaceOrder.builder()
                .id(1L).orderGroup(group).orderCode("MO-TEST").status(status)
                .buyerUser(buyer).farmerUser(farmer)
                .paymentMethod(MarketplacePaymentMethod.BANK_TRANSFER)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.NOT_REQUIRED)
                .shippingRecipientName("Buyer").shippingPhone("0909").shippingAddressLine("123 Rd")
                .subtotal(new BigDecimal("200000")).shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("220000")).items(List.of()).build();
    }
}
