package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateReviewRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateReviewRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceReviewResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProductReview;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceAddressRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartItemRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderGroupRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderItemRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Marketplace Review & Rating")
class MarketplaceReviewIntegrationTest {

    @Mock private MarketplaceProductRepository marketplaceProductRepository;
    @Mock private MarketplaceCartRepository marketplaceCartRepository;
    @Mock private MarketplaceCartItemRepository marketplaceCartItemRepository;
    @Mock private MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;
    @Mock private MarketplaceOrderItemRepository marketplaceOrderItemRepository;
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
    private User otherBuyer;
    private Farm farm;
    private MarketplaceProduct product;
    private MarketplaceOrder completedOrder;
    private MarketplaceOrder pendingOrder;
    private MarketplaceOrderItem orderItem;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(10L).username("buyer-10").fullName("Test Buyer").build();
        otherBuyer = User.builder().id(99L).username("other-99").fullName("Other Buyer").build();

        farm = Farm.builder().id(1).name("Green Farm").user(User.builder().id(20L).build()).build();

        ProductWarehouseLot lot = ProductWarehouseLot.builder()
                .id(1).lotCode("LOT-1").productName("Rice").farm(farm)
                .season(Season.builder().id(1).seasonName("Spring 2026")
                        .plot(Plot.builder().id(1).farm(farm).plotName("Plot A").build()).build())
                .plot(Plot.builder().id(1).farm(farm).plotName("Plot A").build())
                .unit("kg").initialQuantity(new BigDecimal("100"))
                .onHandQuantity(new BigDecimal("100"))
                .status(ProductWarehouseLotStatus.IN_STOCK).build();

        product = MarketplaceProduct.builder()
                .id(200L).slug("fresh-rice").name("Fresh Rice")
                .price(new BigDecimal("50000")).unit("kg")
                .stockQuantity(new BigDecimal("50"))
                .status(MarketplaceProductStatus.PUBLISHED)
                .traceable(false).lot(lot).farm(farm)
                .season(lot.getSeason())
                .farmerUser(User.builder().id(20L).username("farmer-20").build())
                .build();

        completedOrder = MarketplaceOrder.builder()
                .id(300L).orderCode("MO-300")
                .status(MarketplaceOrderStatus.COMPLETED)
                .buyerUser(buyer)
                .farmerUser(User.builder().id(20L).build())
                .paymentMethod(MarketplacePaymentMethod.COD)
                .shippingRecipientName("Buyer").shippingPhone("0909000000")
                .shippingAddressLine("123 Road")
                .subtotal(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("120000"))
                .build();

        orderItem = MarketplaceOrderItem.builder()
                .id(1L).order(completedOrder).product(product)
                .quantity(new BigDecimal("2")).unitPriceSnapshot(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("100000")).traceableSnapshot(false)
                .productNameSnapshot("Fresh Rice").productSlugSnapshot("fresh-rice")
                .build();
        completedOrder.setItems(List.of(orderItem));

        pendingOrder = MarketplaceOrder.builder()
                .id(400L).orderCode("MO-400")
                .status(MarketplaceOrderStatus.PENDING_PAYMENT)
                .buyerUser(buyer)
                .farmerUser(User.builder().id(20L).build())
                .paymentMethod(MarketplacePaymentMethod.COD)
                .shippingRecipientName("Buyer").shippingPhone("0909000000")
                .shippingAddressLine("123 Road")
                .subtotal(new BigDecimal("100000"))
                .shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("120000"))
                .build();
        pendingOrder.setItems(List.of(orderItem));

        // Default stubs for rating recalculation to prevent NPE
        lenient().when(marketplaceProductRepository.findById(200L)).thenReturn(Optional.of(product));
        lenient().when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        lenient().when(marketplaceProductReviewRepository.aggregateRatingByProductId(anyLong()))
                .thenReturn(buildRatingProjection(4.0, 1L));
        lenient().when(marketplaceProductReviewRepository.aggregateRatingByFarmId(anyInt()))
                .thenReturn(buildRatingProjection(4.0, 1L));
        lenient().when(marketplaceProductRepository.save(any(MarketplaceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(farmRepository.save(any(Farm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(anyLong(), anyLong()))
                .thenReturn(List.of());
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. CREATE REVIEW
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createReview()")
    class CreateReviewTests {

        @Test
        @DisplayName("Should block review if order is not COMPLETED")
        void shouldBlockReviewIfOrderNotCompleted() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(400L, 10L))
                    .thenReturn(Optional.of(pendingOrder));

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(1L, 5, "Great!");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.createReview(400L, request));
            assertEquals(ErrorCode.MARKETPLACE_REVIEW_ORDER_NOT_COMPLETED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should block review if buyer did not purchase (order not found)")
        void shouldBlockReviewIfBuyerDidNotPurchase() {
            when(currentUserService.getCurrentUser()).thenReturn(otherBuyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 99L))
                    .thenReturn(Optional.empty());

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(1L, 5, "Great!");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.createReview(300L, request));
            assertEquals(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should block review if order item does not belong to order")
        void shouldBlockReviewIfOrderItemNotInOrder() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L))
                    .thenReturn(Optional.of(completedOrder));
            when(marketplaceOrderItemRepository.findByIdAndOrder_Id(999L, 300L))
                    .thenReturn(Optional.empty());

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(999L, 5, "Great!");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.createReview(300L, request));
            assertEquals(ErrorCode.MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should block duplicate review on same order item")
        void shouldBlockDuplicateReview() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L))
                    .thenReturn(Optional.of(completedOrder));
            when(marketplaceOrderItemRepository.findByIdAndOrder_Id(1L, 300L))
                    .thenReturn(Optional.of(orderItem));
            when(marketplaceProductReviewRepository.existsByOrderItem_IdAndBuyerUser_Id(1L, 10L))
                    .thenReturn(true);

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(1L, 4, "Nice");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.createReview(300L, request));
            assertEquals(ErrorCode.MARKETPLACE_REVIEW_ALREADY_EXISTS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should create review successfully for completed order")
        void shouldCreateReviewSuccessfully() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L))
                    .thenReturn(Optional.of(completedOrder));
            when(marketplaceOrderItemRepository.findByIdAndOrder_Id(1L, 300L))
                    .thenReturn(Optional.of(orderItem));
            when(marketplaceProductReviewRepository.existsByOrderItem_IdAndBuyerUser_Id(1L, 10L))
                    .thenReturn(false);

            MarketplaceProductReview savedReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(5).comment("Excellent rice!")
                    .hidden(false).build();
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenReturn(savedReview);

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(1L, 5, "Excellent rice!");

            MarketplaceReviewResponse response = marketplaceService.createReview(300L, request);

            assertNotNull(response);
            assertEquals(50L, response.id());
            assertEquals(5, response.rating());
            assertEquals("Excellent rice!", response.comment());
            assertEquals(200L, response.productId());
            assertEquals(300L, response.orderId());
            assertEquals(1L, response.orderItemId());
            assertEquals(10L, response.buyerUserId());
            assertEquals("Test Buyer", response.buyerDisplayName());
        }

        @Test
        @DisplayName("Should create review with optional comment (null)")
        void shouldCreateReviewWithOptionalComment() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L))
                    .thenReturn(Optional.of(completedOrder));
            when(marketplaceOrderItemRepository.findByIdAndOrder_Id(1L, 300L))
                    .thenReturn(Optional.of(orderItem));
            when(marketplaceProductReviewRepository.existsByOrderItem_IdAndBuyerUser_Id(1L, 10L))
                    .thenReturn(false);

            MarketplaceProductReview savedReview = MarketplaceProductReview.builder()
                    .id(51L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(4).comment(null)
                    .hidden(false).build();
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenReturn(savedReview);

            MarketplaceCreateReviewRequest request = new MarketplaceCreateReviewRequest(1L, 4, null);

            MarketplaceReviewResponse response = marketplaceService.createReview(300L, request);

            assertNotNull(response);
            assertEquals(4, response.rating());
            assertEquals(null, response.comment());
        }

        @Test
        @DisplayName("Should recalculate product and farm avg rating after create")
        void shouldRecalculateRatingsAfterCreate() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L))
                    .thenReturn(Optional.of(completedOrder));
            when(marketplaceOrderItemRepository.findByIdAndOrder_Id(1L, 300L))
                    .thenReturn(Optional.of(orderItem));
            when(marketplaceProductReviewRepository.existsByOrderItem_IdAndBuyerUser_Id(1L, 10L))
                    .thenReturn(false);

            MarketplaceProductReview savedReview = MarketplaceProductReview.builder()
                    .id(52L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(4).hidden(false).build();
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenReturn(savedReview);

            // Set up rating recalculation expectations
            when(marketplaceProductReviewRepository.aggregateRatingByProductId(200L))
                    .thenReturn(buildRatingProjection(4.5, 2L));
            when(marketplaceProductReviewRepository.aggregateRatingByFarmId(1))
                    .thenReturn(buildRatingProjection(4.5, 2L));

            marketplaceService.createReview(300L, new MarketplaceCreateReviewRequest(1L, 4, null));

            // Verify denormalized ratings were updated
            assertEquals(4.5, product.getAverageRating());
            assertEquals(2, product.getRatingCount());
            assertEquals(4.5, farm.getAverageRating());
            assertEquals(2, farm.getRatingCount());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. EDIT REVIEW
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("editReview()")
    class EditReviewTests {

        @Test
        @DisplayName("Should edit own review successfully")
        void shouldEditOwnReview() {
            MarketplaceProductReview existingReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(3).comment("OK")
                    .hidden(false).build();

            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(existingReview));
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MarketplaceUpdateReviewRequest request = new MarketplaceUpdateReviewRequest(5, "Amazing!");

            MarketplaceReviewResponse response = marketplaceService.editReview(50L, request);

            assertEquals(5, response.rating());
            assertEquals("Amazing!", response.comment());
        }

        @Test
        @DisplayName("Should block edit of another buyer's review")
        void shouldBlockEditOfAnotherBuyersReview() {
            MarketplaceProductReview existingReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(3).comment("OK")
                    .hidden(false).build();

            when(currentUserService.getCurrentUserId()).thenReturn(99L);
            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(existingReview));

            MarketplaceUpdateReviewRequest request = new MarketplaceUpdateReviewRequest(1, "Bad!");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.editReview(50L, request));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should not recalculate ratings if only comment changed")
        void shouldNotRecalculateIfOnlyCommentChanged() {
            MarketplaceProductReview existingReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(4).comment("OK")
                    .hidden(false).build();

            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(existingReview));
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Only update comment, keep same rating
            MarketplaceUpdateReviewRequest request = new MarketplaceUpdateReviewRequest(4, "Updated comment");

            marketplaceService.editReview(50L, request);

            // Rating didn't change, so no recalculation should happen
            verify(marketplaceProductReviewRepository, never()).aggregateRatingByProductId(anyLong());
        }

        @Test
        @DisplayName("Should return NOT_FOUND for non-existent review")
        void shouldReturnNotFoundForNonExistentReview() {
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceProductReviewRepository.findById(999L)).thenReturn(Optional.empty());

            MarketplaceUpdateReviewRequest request = new MarketplaceUpdateReviewRequest(5, "Wow");

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.editReview(999L, request));
            assertEquals(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. DELETE REVIEW
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteReview()")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete own review and recalculate ratings")
        void shouldDeleteOwnReview() {
            MarketplaceProductReview existingReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(3).hidden(false).build();

            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(existingReview));

            // After deletion, ratings drop to 0
            when(marketplaceProductReviewRepository.aggregateRatingByProductId(200L))
                    .thenReturn(buildRatingProjection(0.0, 0L));
            when(marketplaceProductReviewRepository.aggregateRatingByFarmId(1))
                    .thenReturn(buildRatingProjection(0.0, 0L));

            marketplaceService.deleteReview(50L);

            verify(marketplaceProductReviewRepository).delete(existingReview);
            assertEquals(0.0, product.getAverageRating());
            assertEquals(0, product.getRatingCount());
            assertEquals(0.0, farm.getAverageRating());
            assertEquals(0, farm.getRatingCount());
        }

        @Test
        @DisplayName("Should block delete of another buyer's review")
        void shouldBlockDeleteOfAnotherBuyersReview() {
            MarketplaceProductReview existingReview = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(3).hidden(false).build();

            when(currentUserService.getCurrentUserId()).thenReturn(99L);
            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(existingReview));

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.deleteReview(50L));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ADMIN MODERATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("admin moderation")
    class AdminModerationTests {

        @Test
        @DisplayName("Should hide review (admin)")
        void shouldAdminHideReview() {
            MarketplaceProductReview review = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(4).hidden(false).build();

            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(review));
            when(marketplaceProductReviewRepository.save(any(MarketplaceProductReview.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MarketplaceReviewResponse response = marketplaceService.adminHideReview(50L);

            assertTrue(response.hidden());
            assertEquals(true, review.getHidden());
        }

        @Test
        @DisplayName("Should permanently delete review (admin)")
        void shouldAdminDeleteReview() {
            MarketplaceProductReview review = MarketplaceProductReview.builder()
                    .id(50L).product(product).order(completedOrder).orderItem(orderItem)
                    .buyerUser(buyer).rating(4).hidden(false).build();

            when(marketplaceProductReviewRepository.findById(50L)).thenReturn(Optional.of(review));

            when(marketplaceProductReviewRepository.aggregateRatingByProductId(200L))
                    .thenReturn(buildRatingProjection(0.0, 0L));
            when(marketplaceProductReviewRepository.aggregateRatingByFarmId(1))
                    .thenReturn(buildRatingProjection(0.0, 0L));

            marketplaceService.adminDeleteReview(50L);

            verify(marketplaceProductReviewRepository).delete(review);
        }

        @Test
        @DisplayName("Should return NOT_FOUND when hiding non-existent review")
        void shouldReturnNotFoundForHideNonExistent() {
            when(marketplaceProductReviewRepository.findById(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> marketplaceService.adminHideReview(999L));
            assertEquals(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private MarketplaceProductReviewRepository.SingleProductRatingProjection buildRatingProjection(
            Double avg, Long count) {
        return new MarketplaceProductReviewRepository.SingleProductRatingProjection() {
            @Override
            public Double getAverageRating() {
                return avg;
            }

            @Override
            public Long getRatingCount() {
                return count;
            }
        };
    }
}
