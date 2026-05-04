package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceRejectPaymentProofRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplacePaymentProofResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCart;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderGroup;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceAddressRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartItemRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderGroupRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductReviewRepository;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PaymentProofIntegrationTest {

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

    @InjectMocks
    private MarketplaceService marketplaceService;

    private User buyer;
    private User admin;
    private User farmer;
    private MarketplaceOrderGroup orderGroup;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(10L).username("buyer-1").fullName("Buyer One").build();
        admin = User.builder().id(1L).username("admin-1").fullName("Admin One").build();
        farmer = User.builder().id(20L).username("farmer-1").fullName("Farmer One").build();
        orderGroup = MarketplaceOrderGroup.builder().id(5L).groupCode("MOG-PROOF-TEST").build();

        lenient().when(marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(anyLong(), anyLong()))
                .thenReturn(List.of());
    }

    private MarketplaceOrder buildBankTransferOrder(MarketplaceOrderStatus status,
                                                     MarketplacePaymentVerificationStatus pvStatus) {
        ProductWarehouseLot lot = buildLot(1, "10");
        MarketplaceProduct product = buildProduct(200L, "rice", "Rice", new BigDecimal("100000"), lot, farmer.getId());

        MarketplaceOrder order = MarketplaceOrder.builder()
                .id(300L)
                .orderGroup(orderGroup)
                .orderCode("MO-PROOF-1")
                .status(status)
                .buyerUser(buyer)
                .farmerUser(farmer)
                .paymentMethod(MarketplacePaymentMethod.BANK_TRANSFER)
                .paymentVerificationStatus(pvStatus)
                .shippingRecipientName("Buyer")
                .shippingPhone("0909000000")
                .shippingAddressLine("123 Road")
                .subtotal(new BigDecimal("200000"))
                .shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("220000"))
                .build();

        MarketplaceOrderItem item = MarketplaceOrderItem.builder()
                .id(1L).order(order).product(product).lot(lot)
                .quantity(new BigDecimal("2")).unitPriceSnapshot(new BigDecimal("100000"))
                .lineTotal(new BigDecimal("200000")).traceableSnapshot(false)
                .productNameSnapshot("Rice").productSlugSnapshot("rice")
                .build();
        order.setItems(List.of(item));
        return order;
    }

    // ─── Integration Flow Tests ─────────────────────────────────

    @Nested
    @DisplayName("upload → verify flow")
    class UploadVerifyFlowTests {

        @Test
        @DisplayName("verifyAdminPaymentProof auto-advances order to PAYMENT_VERIFIED")
        void uploadProof_ThenVerify_OrderAdvancesToPaymentVerified() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.PAYMENT_SUBMITTED,
                    MarketplacePaymentVerificationStatus.SUBMITTED);
            order.setPaymentProofStoragePath("/some/path/proof.png");
            order.setPaymentProofFileName("proof.png");
            order.setPaymentProofContentType("image/png");
            order.setPaymentProofUploadedAt(LocalDateTime.now());

            when(currentUserService.getCurrentUser()).thenReturn(admin);
            when(marketplaceOrderRepository.findByIdWithItems(300L)).thenReturn(Optional.of(order));
            when(marketplaceOrderRepository.save(any(MarketplaceOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MarketplacePaymentProofResponse response = marketplaceService.verifyAdminPaymentProof(300L);

            assertNotNull(response);
            assertEquals(MarketplacePaymentVerificationStatus.VERIFIED, response.verificationStatus());
            assertEquals(MarketplaceOrderStatus.PAYMENT_VERIFIED, order.getStatus());
            assertEquals(admin.getId(), response.verifiedBy());
            verify(marketplaceOrderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("upload → reject flow")
    class UploadRejectFlowTests {

        @Test
        @DisplayName("rejectAdminPaymentProof sets REJECTED with reason")
        void uploadProof_ThenReject_ProofStatusRejected() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.PAYMENT_SUBMITTED,
                    MarketplacePaymentVerificationStatus.SUBMITTED);
            order.setPaymentProofStoragePath("/some/path/proof.png");
            order.setPaymentProofFileName("proof.png");
            order.setPaymentProofContentType("image/png");
            order.setPaymentProofUploadedAt(LocalDateTime.now());

            when(currentUserService.getCurrentUser()).thenReturn(admin);
            when(marketplaceOrderRepository.findByIdWithItems(300L)).thenReturn(Optional.of(order));
            when(marketplaceOrderRepository.save(any(MarketplaceOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MarketplaceRejectPaymentProofRequest request = new MarketplaceRejectPaymentProofRequest(
                    "Blurry image, cannot read transaction ID");

            MarketplacePaymentProofResponse response = marketplaceService.rejectAdminPaymentProof(300L, request);

            assertNotNull(response);
            assertEquals(MarketplacePaymentVerificationStatus.REJECTED, response.verificationStatus());
            assertEquals("Blurry image, cannot read transaction ID", response.verificationNote());
            assertEquals(admin.getId(), response.verifiedBy());
            verify(marketplaceOrderRepository).save(order);
        }
    }

    // ─── Validation Tests ───────────────────────────────────────

    @Nested
    @DisplayName("validation constraints")
    class ValidationTests {

        @Test
        @DisplayName("uploadPaymentProof blocks when order is PAYMENT_VERIFIED")
        void uploadProof_WhenOrderNotPendingPayment_ThrowsException() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.PAYMENT_VERIFIED,
                    MarketplacePaymentVerificationStatus.VERIFIED);

            when(currentUserService.getCurrentUserId()).thenReturn(buyer.getId());
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, buyer.getId()))
                    .thenReturn(Optional.of(order));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "proof.png", "image/png", "fake-content".getBytes());

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.uploadPaymentProof(300L, file));
            assertEquals(ErrorCode.MARKETPLACE_PAYMENT_PROOF_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("uploadPaymentProof blocks when order is CONFIRMED (beyond PAYMENT_SUBMITTED)")
        void uploadProof_WhenOrderConfirmed_ThrowsException() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.CONFIRMED,
                    MarketplacePaymentVerificationStatus.VERIFIED);

            when(currentUserService.getCurrentUserId()).thenReturn(buyer.getId());
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, buyer.getId()))
                    .thenReturn(Optional.of(order));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "proof.png", "image/png", "fake-content".getBytes());

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.uploadPaymentProof(300L, file));
            assertEquals(ErrorCode.MARKETPLACE_PAYMENT_PROOF_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("rejectAdminPaymentProof requires non-empty reason")
        void rejectProof_WithEmptyReason_ThrowsException() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.PAYMENT_SUBMITTED,
                    MarketplacePaymentVerificationStatus.SUBMITTED);
            order.setPaymentProofStoragePath("/some/path/proof.png");

            lenient().when(currentUserService.getCurrentUser()).thenReturn(admin);
            when(marketplaceOrderRepository.findByIdWithItems(300L)).thenReturn(Optional.of(order));

            MarketplaceRejectPaymentProofRequest request = new MarketplaceRejectPaymentProofRequest("   ");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.rejectAdminPaymentProof(300L, request));
            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        }
    }

    // ─── Security Tests ─────────────────────────────────────────

    @Nested
    @DisplayName("security constraints")
    class SecurityTests {

        @Test
        @DisplayName("buyer cannot access another buyer's payment proof")
        void getPaymentProof_BuyerCannotAccessAnotherBuyerProof() {
            Long otherBuyerId = 99L;

            when(currentUserService.getCurrentUserId()).thenReturn(otherBuyerId);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, otherBuyerId))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.getPaymentProof(300L));
            assertEquals(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("verify requires proof in SUBMITTED status — blocks AWAITING_PROOF")
        void verifyProof_WhenNotSubmitted_ThrowsException() {
            MarketplaceOrder order = buildBankTransferOrder(
                    MarketplaceOrderStatus.PENDING_PAYMENT,
                    MarketplacePaymentVerificationStatus.AWAITING_PROOF);
            order.setPaymentProofStoragePath("/some/path/proof.png");

            lenient().when(currentUserService.getCurrentUser()).thenReturn(admin);
            when(marketplaceOrderRepository.findByIdWithItems(300L)).thenReturn(Optional.of(order));

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.verifyAdminPaymentProof(300L));
            assertEquals(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID, ex.getErrorCode());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────

    private ProductWarehouseLot buildLot(int id, String onHand) {
        Farm farm = Farm.builder().id(1).name("Green Farm").user(farmer).build();
        Plot plot = Plot.builder().id(1).farm(farm).plotName("Plot A").build();
        Season season = Season.builder().id(1).seasonName("Spring 2026").plot(plot).build();
        return ProductWarehouseLot.builder()
                .id(id).lotCode("LOT-" + id).productName("Produce " + id)
                .farm(farm).season(season).plot(plot).unit("kg")
                .initialQuantity(new BigDecimal(onHand))
                .onHandQuantity(new BigDecimal(onHand))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .build();
    }

    private MarketplaceProduct buildProduct(Long id, String slug, String name,
                                             BigDecimal price, ProductWarehouseLot lot, Long farmerId) {
        return MarketplaceProduct.builder()
                .id(id).slug(slug).name(name).price(price).unit("kg")
                .stockQuantity(lot.getOnHandQuantity())
                .status(MarketplaceProductStatus.PUBLISHED)
                .traceable(false).lot(lot).farm(lot.getFarm()).season(lot.getSeason())
                .farmerUser(User.builder().id(farmerId).username("farmer-" + farmerId).build())
                .build();
    }
}
