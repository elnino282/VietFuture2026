package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collection;
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
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceFarmerProductUpsertRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceMergeCartRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateProductStatusRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCartResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCreateOrderResultResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCart;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCartItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderGroup;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProductReview;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentMethod;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    private MarketplaceProductRepository marketplaceProductRepository;

    @Mock
    private MarketplaceCartRepository marketplaceCartRepository;

    @Mock
    private MarketplaceCartItemRepository marketplaceCartItemRepository;

    @Mock
    private MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;

    @Mock
    private MarketplaceOrderItemRepository marketplaceOrderItemRepository;

    @Mock
    private MarketplaceOrderRepository marketplaceOrderRepository;

    @Mock
    private MarketplaceAddressRepository marketplaceAddressRepository;

    @Mock
    private MarketplaceProductReviewRepository marketplaceProductReviewRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private ProductWarehouseLotRepository productWarehouseLotRepository;

    @Mock
    private ProductWarehouseTransactionRepository productWarehouseTransactionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MarketplaceService marketplaceService;

    private User buyer;
    private MarketplaceCart cart;
    private ProductWarehouseLot lot;
    private MarketplaceProduct product;

    @BeforeEach
    void setUp() {
        buyer = User.builder()
                .id(10L)
                .username("market-user")
                .fullName("Market User")
                .build();

        cart = MarketplaceCart.builder()
                .id(100L)
                .user(buyer)
                .build();

        lot = buildLot(1, "10");
        product = buildProduct(200L, "rice-bag", "Rice Bag", new BigDecimal("100000"), lot, 20L);

        lenient().when(marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(anyLong(), anyLong()))
                .thenReturn(List.of());
        lenient().when(marketplaceProductRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productWarehouseTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productWarehouseLotRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("listProducts()")
    class ListProductsTests {

        @Test
        void listProducts_UsesActiveAndLegacyPublishedStatuses() {
            MarketplaceProduct activeProduct = buildProduct(
                    300L,
                    "active-rice",
                    "Active Rice",
                    new BigDecimal("120000"),
                    buildLot(3, "12"),
                    20L);
            activeProduct.setStatus(MarketplaceProductStatus.ACTIVE);

            when(marketplaceProductRepository.searchPublished(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeProduct)));
            when(marketplaceProductReviewRepository.aggregateRatingsByProductIds(List.of(300L))).thenReturn(List.of());

            var response = marketplaceService.listProducts(null, null, null, null, null, null, null, null, 0, 20);

            ArgumentCaptor<Collection<MarketplaceProductStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(marketplaceProductRepository).searchPublished(
                    statusesCaptor.capture(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(Pageable.class));
            assertTrue(statusesCaptor.getValue().contains(MarketplaceProductStatus.ACTIVE));
            assertTrue(statusesCaptor.getValue().contains(MarketplaceProductStatus.PUBLISHED));
            assertEquals(MarketplaceProductStatus.ACTIVE, response.getItems().getFirst().status());
        }

        @Test
        void listProducts_PassesFarmIdToPublishedSearch() {
            when(marketplaceProductRepository.searchPublished(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            marketplaceService.listProducts(null, null, null, null, null, null, null, 7, 0, 20);

            verify(marketplaceProductRepository).searchPublished(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    org.mockito.ArgumentMatchers.eq(7),
                    any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("listFarms()")
    class ListFarmsTests {

        @Test
        void listFarms_DoesNotPassProductNameSortToDistinctFarmQuery() {
            when(marketplaceProductRepository.searchDistinctFarmsWithPublishedProducts(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            marketplaceService.listFarms(" farm ", " Can Tho ", 0, 20);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(marketplaceProductRepository).searchDistinctFarmsWithPublishedProducts(
                    any(),
                    any(),
                    any(),
                    pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(20, pageable.getPageSize());
            assertTrue(pageable.getSort().isUnsorted());
        }
    }

    @Nested
    @DisplayName("getFarmDetail()")
    class GetFarmDetailTests {

        @Test
        void getFarmDetail_ReturnsFarmMarketplaceDisplayFields() {
            User owner = User.builder()
                    .id(20L)
                    .username("farmer-20")
                    .fullName("Farmer Twenty")
                    .phone("0909000000")
                    .build();
            Farm farm = Farm.builder()
                    .id(7)
                    .name("Trace Farm")
                    .user(owner)
                    .active(true)
                    .averageRating(4.7)
                    .ratingCount(12)
                    .build();

            when(farmRepository.findById(7)).thenReturn(Optional.of(farm));
            when(marketplaceProductRepository.countSellableByFarmIdAndStatusIn(any(), any())).thenReturn(3L);
            when(marketplaceProductRepository.existsSellableTraceableByFarmIdAndStatusIn(any(), any())).thenReturn(true);
            when(marketplaceProductRepository.findSellableByFarmIdAndStatusInOrderByPublishedAtDescIdDesc(
                    any(),
                    any(),
                    any(Pageable.class)))
                    .thenReturn(List.of());

            var response = marketplaceService.getFarmDetail(7);

            assertEquals(true, response.active());
            assertEquals(4.7, response.ratingAverage());
            assertEquals(12, response.ratingCount());
            assertEquals(true, response.hasTraceableProducts());
            assertEquals(3L, response.productCount());
            assertEquals(20L, response.ownerUserId());
        }
    }

    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        void createOrder_MissingIdempotencyKey_ThrowsException() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);

            MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                    MarketplacePaymentMethod.COD,
                    null,
                    "Buyer Name",
                    "0909000000",
                    "123 Road",
                    null,
                    null);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.createOrder(request, null));
            assertEquals(ErrorCode.MARKETPLACE_IDEMPOTENCY_KEY_REQUIRED, ex.getErrorCode());
        }

        @Test
        void createOrder_CartEmpty_ThrowsException() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of());

            MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                    MarketplacePaymentMethod.COD,
                    null,
                    "Buyer Name",
                    "0909000000",
                    "123 Road",
                    null,
                    "idem-empty");

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.createOrder(request, null));
            assertEquals(ErrorCode.MARKETPLACE_CART_EMPTY, ex.getErrorCode());
        }

        @Test
        void createOrder_InsufficientStock_ThrowsException() throws Exception {
            ProductWarehouseLot lowStockLot = buildLot(1, "1");
            MarketplaceProduct lowStockProduct = buildProduct(200L, "rice-bag", "Rice Bag", new BigDecimal("100000"), lowStockLot, 20L);

            MarketplaceCartItem cartItem = MarketplaceCartItem.builder()
                    .id(1000L)
                    .cart(cart)
                    .product(lowStockProduct)
                    .quantity(new BigDecimal("2"))
                    .unitPriceSnapshot(lowStockProduct.getPrice())
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of(cartItem));
            when(marketplaceOrderGroupRepository.findByBuyerUser_IdAndIdempotencyKey(10L, "idem-stock"))
                    .thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"k\":\"v\"}");
            when(marketplaceProductRepository.findAllByIdInForUpdate(List.of(200L))).thenReturn(List.of(lowStockProduct));
            when(productWarehouseLotRepository.findAllByIdInForUpdate(List.of(1))).thenReturn(List.of(lowStockLot));

            MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                    MarketplacePaymentMethod.COD,
                    null,
                    "Buyer Name",
                    "0909000000",
                    "123 Road",
                    null,
                    "idem-stock");

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.createOrder(request, null));
            assertEquals(ErrorCode.MARKETPLACE_STOCK_CONFLICT, ex.getErrorCode());
        }

        @Test
        void createOrder_MultiFarmerSplit_Success() throws Exception {
            ProductWarehouseLot lotA = buildLot(1, "10");
            ProductWarehouseLot lotB = buildLot(2, "20");

            MarketplaceProduct productA = buildProduct(200L, "rice-bag", "Rice Bag", new BigDecimal("100000"), lotA, 20L);
            MarketplaceProduct productB = buildProduct(201L, "tomato", "Tomato", new BigDecimal("50000"), lotB, 21L);

            MarketplaceCartItem itemA = MarketplaceCartItem.builder()
                    .id(1L)
                    .cart(cart)
                    .product(productA)
                    .quantity(new BigDecimal("2"))
                    .unitPriceSnapshot(productA.getPrice())
                    .build();

            MarketplaceCartItem itemB = MarketplaceCartItem.builder()
                    .id(2L)
                    .cart(cart)
                    .product(productB)
                    .quantity(new BigDecimal("1"))
                    .unitPriceSnapshot(productB.getPrice())
                    .build();

            MarketplaceOrderGroup savedGroup = MarketplaceOrderGroup.builder()
                    .id(99L)
                    .groupCode("MOG-SPLIT")
                    .buyerUser(buyer)
                    .idempotencyKey("idem-split")
                    .requestFingerprint("fp")
                    .build();

            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceCartItemRepository.findByCartIdWithProductForUpdate(100L)).thenReturn(List.of(itemA, itemB));
            when(marketplaceOrderGroupRepository.findByBuyerUser_IdAndIdempotencyKey(10L, "idem-split"))
                    .thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"k\":\"v\"}");
            when(marketplaceProductRepository.findAllByIdInForUpdate(List.of(200L, 201L)))
                    .thenReturn(List.of(productA, productB));
            when(productWarehouseLotRepository.findAllByIdInForUpdate(List.of(1, 2))).thenReturn(List.of(lotA, lotB));
            when(marketplaceOrderGroupRepository.save(any(MarketplaceOrderGroup.class))).thenReturn(savedGroup);
            when(marketplaceOrderRepository.save(any(MarketplaceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MarketplaceCreateOrderRequest request = new MarketplaceCreateOrderRequest(
                    MarketplacePaymentMethod.COD,
                    null,
                    "Buyer Name",
                    "0909000000",
                    "123 Road",
                    null,
                    "idem-split");

            MarketplaceCreateOrderResultResponse response = marketplaceService.createOrder(request, null);

            assertNotNull(response);
            assertEquals(2, response.splitCount());
            assertEquals("MOG-SPLIT", response.orderGroupCode());
            assertEquals(0, productA.getStockQuantity().compareTo(new BigDecimal("8")));
            assertEquals(0, productB.getStockQuantity().compareTo(new BigDecimal("19")));
            assertEquals(0, lotA.getOnHandQuantity().compareTo(new BigDecimal("8")));
            assertEquals(0, lotB.getOnHandQuantity().compareTo(new BigDecimal("19")));
            verify(marketplaceCartItemRepository).deleteAllByCartId(100L);
        }
    }

    @Nested
    @DisplayName("cart validation")
    class CartValidationTests {

        @Test
        void addCartItem_PublishedProduct_AddsSuccessfully() {
            product.setStatus(MarketplaceProductStatus.PUBLISHED);
            stubCartProduct(product);
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.empty());
            when(marketplaceCartItemRepository.save(any(MarketplaceCartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(marketplaceCartItemRepository.findByCartIdWithProduct(100L)).thenReturn(List.of(cartItem(product, new BigDecimal("2"))));

            MarketplaceCartResponse response = marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, new BigDecimal("2")));

            assertEquals(0, response.itemCount().compareTo(new BigDecimal("2")));
            assertEquals(1, response.items().size());
            assertEquals(0, response.items().getFirst().quantity().compareTo(new BigDecimal("2")));
        }

        @Test
        void addCartItem_ActiveProduct_AddsSuccessfully() {
            product.setStatus(MarketplaceProductStatus.ACTIVE);
            stubCartProduct(product);
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.empty());
            when(marketplaceCartItemRepository.save(any(MarketplaceCartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(marketplaceCartItemRepository.findByCartIdWithProduct(100L)).thenReturn(List.of(cartItem(product, new BigDecimal("1"))));

            MarketplaceCartResponse response = marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, new BigDecimal("1")));

            assertEquals(0, response.itemCount().compareTo(new BigDecimal("1")));
        }

        @Test
        void addCartItem_MissingProduct_ThrowsProductNotFound() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceProductRepository.findByIdWithLotForCartValidation(999L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(999L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void addCartItem_InactiveProduct_ThrowsProductNotAvailable() {
            product.setStatus(MarketplaceProductStatus.INACTIVE);
            stubCartProduct(product);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_NOT_AVAILABLE, ex.getErrorCode());
        }

        @Test
        void addCartItem_ZeroProductStock_ThrowsProductOutOfStock() {
            product.setStockQuantity(BigDecimal.ZERO);
            stubCartProduct(product);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_OUT_OF_STOCK, ex.getErrorCode());
        }

        @Test
        void addCartItem_MissingLot_ThrowsProductLotUnavailable() {
            product.setLot(null);
            stubCartProduct(product);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_LOT_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        void addCartItem_LotNotInStock_ThrowsProductLotUnavailable() {
            lot.setStatus(ProductWarehouseLotStatus.HOLD);
            stubCartProduct(product);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_LOT_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        void addCartItem_ZeroLotOnHand_ThrowsProductLotUnavailable() {
            lot.setOnHandQuantity(BigDecimal.ZERO);
            stubCartProduct(product);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, BigDecimal.ONE)));

            assertEquals(ErrorCode.PRODUCT_LOT_UNAVAILABLE, ex.getErrorCode());
        }

        @Test
        void addCartItem_QuantityGreaterThanAvailableStock_ThrowsInsufficientStock() {
            product.setStockQuantity(new BigDecimal("10"));
            lot.setOnHandQuantity(new BigDecimal("5"));
            stubCartProduct(product);
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, new BigDecimal("6"))));

            assertEquals(ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK, ex.getErrorCode());
            assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode().getCode());
        }

        @Test
        void addCartItem_ExistingItem_ValidatesAndSavesTotalQuantity() {
            stubCartProduct(product);
            MarketplaceCartItem existing = cartItem(product, new BigDecimal("2"));
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.of(existing));
            when(marketplaceCartItemRepository.save(any(MarketplaceCartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(marketplaceCartItemRepository.findByCartIdWithProduct(100L)).thenReturn(List.of(cartItem(product, new BigDecimal("5"))));

            MarketplaceCartResponse response = marketplaceService.addCartItem(
                    new MarketplaceAddCartItemRequest(200L, new BigDecimal("3")));

            assertEquals(0, existing.getQuantity().compareTo(new BigDecimal("5")));
            assertEquals(0, response.itemCount().compareTo(new BigDecimal("5")));
        }

        @Test
        void updateCartItem_QuantityGreaterThanAvailableStock_ThrowsInsufficientStock() {
            product.setStockQuantity(new BigDecimal("4"));
            lot.setOnHandQuantity(new BigDecimal("10"));
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceProductRepository.findByIdWithLotForCartValidation(200L)).thenReturn(Optional.of(product));

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.updateCartItem(
                    200L,
                    new MarketplaceUpdateCartItemRequest(new BigDecimal("5"))));

            assertEquals(ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK, ex.getErrorCode());
        }

        @Test
        void mergeCart_MergedQuantityGreaterThanAvailableStock_ThrowsInsufficientStock() {
            stubCartProduct(product);
            MarketplaceCartItem existing = cartItem(product, new BigDecimal("8"));
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.of(existing));

            MarketplaceMergeCartRequest request = new MarketplaceMergeCartRequest(
                    List.of(new MarketplaceMergeCartRequest.MarketplaceMergeCartItem(200L, new BigDecimal("3"))));

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.mergeCart(request));
            assertEquals(ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("mergeCart()")
    class MergeCartTests {

        @Test
        void mergeCart_MergeExistingItem_ReturnsMergedCart() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceProductRepository.findByIdWithLotForCartValidation(200L))
                    .thenReturn(Optional.of(product));

            MarketplaceCartItem existing = MarketplaceCartItem.builder()
                    .id(1001L)
                    .cart(cart)
                    .product(product)
                    .quantity(new BigDecimal("2"))
                    .unitPriceSnapshot(product.getPrice())
                    .build();

            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.of(existing));
            when(marketplaceCartItemRepository.save(any(MarketplaceCartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MarketplaceCartItem updated = MarketplaceCartItem.builder()
                    .id(1001L)
                    .cart(cart)
                    .product(product)
                    .quantity(new BigDecimal("5"))
                    .unitPriceSnapshot(product.getPrice())
                    .build();
            when(marketplaceCartItemRepository.findByCartIdWithProduct(100L)).thenReturn(List.of(updated));

            MarketplaceMergeCartRequest request = new MarketplaceMergeCartRequest(
                    List.of(new MarketplaceMergeCartRequest.MarketplaceMergeCartItem(200L, new BigDecimal("3"))));

            MarketplaceCartResponse response = marketplaceService.mergeCart(request);

            assertNotNull(response);
            assertEquals(0, response.itemCount().compareTo(new BigDecimal("5")));
            assertEquals(1, response.items().size());
            assertEquals(0, response.items().getFirst().quantity().compareTo(new BigDecimal("5")));
        }

        @Test
        void mergeCart_ExceedStock_ThrowsException() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
            when(marketplaceProductRepository.findByIdWithLotForCartValidation(200L))
                    .thenReturn(Optional.of(product));

            MarketplaceCartItem existing = MarketplaceCartItem.builder()
                    .id(1001L)
                    .cart(cart)
                    .product(product)
                    .quantity(new BigDecimal("8"))
                    .unitPriceSnapshot(product.getPrice())
                    .build();
            when(marketplaceCartItemRepository.findByCart_IdAndProduct_Id(100L, 200L)).thenReturn(Optional.of(existing));

            MarketplaceMergeCartRequest request = new MarketplaceMergeCartRequest(
                    List.of(new MarketplaceMergeCartRequest.MarketplaceMergeCartItem(200L, new BigDecimal("3"))));

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.mergeCart(request));
            assertEquals(ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrderTests {

        @Test
        void cancelOrder_WithPendingStatus_RestoresLotStock() {
            when(currentUserService.getCurrentUser()).thenReturn(buyer);
            when(currentUserService.getCurrentUserId()).thenReturn(10L);

            MarketplaceOrderGroup group = MarketplaceOrderGroup.builder().id(5L).groupCode("MOG-20260419-ABC").build();
            MarketplaceOrder order = MarketplaceOrder.builder()
                    .id(300L)
                    .orderGroup(group)
                    .orderCode("MO-1")
                    .status(MarketplaceOrderStatus.PENDING_PAYMENT)
                    .buyerUser(buyer)
                    .farmerUser(User.builder().id(20L).build())
                    .paymentMethod(MarketplacePaymentMethod.COD)
                    .shippingRecipientName("Buyer")
                    .shippingPhone("0909000000")
                    .shippingAddressLine("123 Road")
                    .subtotal(new BigDecimal("200000"))
                    .shippingFee(new BigDecimal("20000"))
                    .totalAmount(new BigDecimal("220000"))
                    .build();

            ProductWarehouseLot reservedLot = buildLot(1, "8");
            MarketplaceOrderItem orderItem = MarketplaceOrderItem.builder()
                    .id(1L)
                    .order(order)
                    .product(product)
                    .lot(reservedLot)
                    .quantity(new BigDecimal("2"))
                    .unitPriceSnapshot(new BigDecimal("100000"))
                    .lineTotal(new BigDecimal("200000"))
                    .traceableSnapshot(false)
                    .build();
            order.setItems(List.of(orderItem));

            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(300L, 10L)).thenReturn(Optional.of(order));
            when(marketplaceProductRepository.findAllByIdInForUpdate(List.of(200L))).thenReturn(List.of(product));
            when(productWarehouseLotRepository.findAllByIdInForUpdate(List.of(1))).thenReturn(List.of(reservedLot));
            when(marketplaceOrderRepository.save(any(MarketplaceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            product.setStockQuantity(new BigDecimal("8"));

            MarketplaceOrderResponse response = marketplaceService.cancelOrder(300L);

            assertEquals(MarketplaceOrderStatus.CANCELLED, response.status());
            assertEquals(0, reservedLot.getOnHandQuantity().compareTo(new BigDecimal("10")));
            assertEquals(0, product.getStockQuantity().compareTo(new BigDecimal("10")));
            verify(marketplaceOrderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("buyer order listing")
    class BuyerOrderListingTests {

        @Test
        void listOrders_HydratesCurrentBuyerPageAndPreservesOrder() {
            User farmer = User.builder().id(20L).username("farmer-20").build();
            MarketplaceProduct productA = buildProduct(201L, "rice-a", "Rice A", new BigDecimal("100000"), buildLot(11, "8"), 20L);
            MarketplaceProduct productB = buildProduct(202L, "rice-b", "Rice B", new BigDecimal("120000"), buildLot(12, "9"), 20L);
            MarketplaceOrder orderA = buildOrder(1L, "MO-1", "MOG-1", buyer, farmer, productA, MarketplaceOrderStatus.COMPLETED, 101L);
            MarketplaceOrder orderB = buildOrder(2L, "MO-2", "MOG-2", buyer, farmer, productB, MarketplaceOrderStatus.PENDING_PAYMENT, 102L);
            MarketplaceProductReview review = MarketplaceProductReview.builder()
                    .id(900L)
                    .order(orderA)
                    .orderItem(orderA.getItems().getFirst())
                    .product(productA)
                    .buyerUser(buyer)
                    .rating(5)
                    .build();

            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceOrderRepository.findBuyerOrderIdsByStatus(
                    anyLong(),
                    any(MarketplaceOrderStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(2L, 1L), Pageable.ofSize(2).withPage(1), 5));
            when(marketplaceOrderRepository.findByIdsWithResponseGraph(List.of(2L, 1L)))
                    .thenReturn(List.of(orderA, orderB));
            when(marketplaceProductReviewRepository.findByOrder_IdInAndBuyerUser_Id(List.of(2L, 1L), 10L))
                    .thenReturn(List.of(review));

            var response = marketplaceService.listOrders("PENDING", 1, 2);

            assertEquals(1, response.getPage());
            assertEquals(2, response.getSize());
            assertEquals(5, response.getTotalElements());
            assertEquals(3, response.getTotalPages());
            assertEquals(List.of(2L, 1L), response.getItems().stream().map(MarketplaceOrderResponse::id).toList());

            MarketplaceOrderResponse first = response.getItems().getFirst();
            assertEquals("MOG-2", first.orderGroupCode());
            assertEquals(MarketplacePaymentMethod.COD, first.payment().method());
            assertEquals(MarketplacePaymentVerificationStatus.NOT_REQUIRED, first.payment().verificationStatus());
            assertEquals(true, first.canCancel());
            assertEquals(1, first.items().size());
            assertEquals(202L, first.items().getFirst().productId());
            assertEquals("Rice B", first.items().getFirst().productName());

            MarketplaceOrderResponse second = response.getItems().get(1);
            assertEquals(false, second.canCancel());
            assertEquals(false, second.items().getFirst().canReview());
            assertEquals(900L, second.items().getFirst().reviewId());

            verify(currentUserService).getCurrentUserId();
            verify(marketplaceOrderRepository).findBuyerOrderIdsByStatus(
                    anyLong(),
                    any(MarketplaceOrderStatus.class),
                    any(Pageable.class));
            verify(marketplaceOrderRepository, never()).findAll(any(Pageable.class));
            verify(marketplaceProductReviewRepository).findByOrder_IdInAndBuyerUser_Id(List.of(2L, 1L), 10L);
        }

        @Test
        void listOrders_NoStatus_UsesBuyerScopedIdPage() {
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceOrderRepository.findBuyerOrderIds(anyLong(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            var response = marketplaceService.listOrders(null, 0, 20);

            assertEquals(0, response.getItems().size());
            verify(marketplaceOrderRepository).findBuyerOrderIds(anyLong(), any(Pageable.class));
            verify(marketplaceOrderRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void listOrders_DeliveringStatus_NormalizesToShipped() {
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceOrderRepository.findBuyerOrderIdsByStatus(
                    anyLong(),
                    any(MarketplaceOrderStatus.class),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            marketplaceService.listOrders("DELIVERING", 0, 20);

            verify(marketplaceOrderRepository).findBuyerOrderIdsByStatus(
                    anyLong(),
                    org.mockito.ArgumentMatchers.eq(MarketplaceOrderStatus.SHIPPED),
                    any(Pageable.class));
        }

        @Test
        void listOrders_InvalidStatus_ThrowsBadRequest() {
            AppException ex = assertThrows(AppException.class, () -> marketplaceService.listOrders("NOT_REAL", 0, 20));

            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
            verify(marketplaceOrderRepository, never()).findBuyerOrderIds(anyLong(), any(Pageable.class));
            verify(marketplaceOrderRepository, never()).findBuyerOrderIdsByStatus(
                    anyLong(),
                    any(MarketplaceOrderStatus.class),
                    any(Pageable.class));
        }

        @Test
        void getOrderDetail_OtherBuyerOrder_ThrowsNotFound() {
            when(currentUserService.getCurrentUserId()).thenReturn(10L);
            when(marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(99L, 10L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.getOrderDetail(99L));

            assertEquals(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("farmer products")
    class FarmerProductTests {

        @Test
        void createFarmerProduct_ValidOwnedLot_SavesDraft() {
            User farmer = User.builder().id(20L).build();
            ProductWarehouseLot ownedLot = buildLot(5, "12");

            when(currentUserService.getCurrentUser()).thenReturn(farmer);
            when(productWarehouseLotRepository.findByIdAndFarmUserIdAndStatusAndOnHandQuantityGreaterThan(
                    5,
                    20L,
                    ProductWarehouseLotStatus.IN_STOCK,
                    BigDecimal.ZERO)).thenReturn(Optional.of(ownedLot));
            when(marketplaceProductRepository.findByLot_Id(5)).thenReturn(Optional.empty());
            when(marketplaceProductRepository.save(any(MarketplaceProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MarketplaceFarmerProductUpsertRequest request = new MarketplaceFarmerProductUpsertRequest(
                    "Fresh Rice",
                    "Grain",
                    "A good harvest",
                    "Harvest-backed listing",
                    new BigDecimal("12000"),
                    new BigDecimal("10"),
                    null,
                    null,
                    5);

            MarketplaceProductDetailResponse response = marketplaceService.createFarmerProduct(request);

            assertEquals(MarketplaceProductStatus.DRAFT, response.status());
            assertEquals(Integer.valueOf(5), response.lotId());
            assertEquals(0, response.stockQuantity().compareTo(new BigDecimal("10")));
            assertEquals(0, response.availableQuantity().compareTo(new BigDecimal("10")));
        }

        @Test
        void createFarmerProduct_NonPositiveStock_ThrowsBadRequest() {
            User farmer = User.builder().id(20L).build();
            ProductWarehouseLot ownedLot = buildLot(5, "12");

            when(currentUserService.getCurrentUser()).thenReturn(farmer);
            when(productWarehouseLotRepository.findByIdAndFarmUserIdAndStatusAndOnHandQuantityGreaterThan(
                    5,
                    20L,
                    ProductWarehouseLotStatus.IN_STOCK,
                    BigDecimal.ZERO)).thenReturn(Optional.of(ownedLot));

            MarketplaceFarmerProductUpsertRequest request = new MarketplaceFarmerProductUpsertRequest(
                    "Fresh Rice",
                    "Grain",
                    "A good harvest",
                    "Harvest-backed listing",
                    new BigDecimal("12000"),
                    BigDecimal.ZERO,
                    null,
                    null,
                    5);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.createFarmerProduct(request));
            assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        }

        @Test
        void createFarmerProduct_StockExceedsLot_ThrowsConflict() {
            User farmer = User.builder().id(20L).build();
            ProductWarehouseLot ownedLot = buildLot(5, "12");

            when(currentUserService.getCurrentUser()).thenReturn(farmer);
            when(productWarehouseLotRepository.findByIdAndFarmUserIdAndStatusAndOnHandQuantityGreaterThan(
                    5,
                    20L,
                    ProductWarehouseLotStatus.IN_STOCK,
                    BigDecimal.ZERO)).thenReturn(Optional.of(ownedLot));

            MarketplaceFarmerProductUpsertRequest request = new MarketplaceFarmerProductUpsertRequest(
                    "Fresh Rice",
                    "Grain",
                    "A good harvest",
                    "Harvest-backed listing",
                    new BigDecimal("12000"),
                    new BigDecimal("15"),
                    null,
                    null,
                    5);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.createFarmerProduct(request));
            assertEquals(ErrorCode.MARKETPLACE_STOCK_CONFLICT, ex.getErrorCode());
        }

        @Test
        void getFarmerProductDetail_Owner_ReturnsProduct() {
            MarketplaceProduct ownedProduct = buildProduct(901L, "rice-lot", "Rice Lot", new BigDecimal("15000"), buildLot(5, "12"), 20L);
            ownedProduct.setStatus(MarketplaceProductStatus.DRAFT);

            when(currentUserService.getCurrentUserId()).thenReturn(20L);
            when(marketplaceProductRepository.findById(901L)).thenReturn(Optional.of(ownedProduct));
            when(marketplaceProductReviewRepository.aggregateRatingsByProductIds(List.of(901L))).thenReturn(List.of());

            MarketplaceProductDetailResponse response = marketplaceService.getFarmerProductDetail(901L);

            assertEquals(901L, response.id());
            assertEquals(0, response.stockQuantity().compareTo(new BigDecimal("12")));
        }

        @Test
        void getFarmerProductDetail_NotOwner_ThrowsNotOwner() {
            MarketplaceProduct otherFarmerProduct = buildProduct(901L, "rice-lot", "Rice Lot", new BigDecimal("15000"), buildLot(5, "12"), 99L);

            when(currentUserService.getCurrentUserId()).thenReturn(20L);
            when(marketplaceProductRepository.findById(901L)).thenReturn(Optional.of(otherFarmerProduct));

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.getFarmerProductDetail(901L));
            assertEquals(ErrorCode.NOT_OWNER, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("seller ownership")
    class SellerOwnershipTests {

        @Test
        void updateFarmerProduct_NotOwner_ThrowsNotOwner() {
            when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(20L).build());
            MarketplaceProduct otherFarmerProduct = buildProduct(900L, "other-product", "Other Product", new BigDecimal("10000"), buildLot(9, "5"), 99L);
            otherFarmerProduct.setStatus(MarketplaceProductStatus.DRAFT);
            when(marketplaceProductRepository.findById(900L)).thenReturn(Optional.of(otherFarmerProduct));

            MarketplaceFarmerProductUpsertRequest request = new MarketplaceFarmerProductUpsertRequest(
                    "Updated",
                    "Vegetable",
                    "short",
                    "desc",
                    new BigDecimal("12000"),
                    new BigDecimal("4"),
                    null,
                    null,
                    1);

            AppException ex = assertThrows(AppException.class, () -> marketplaceService.updateFarmerProduct(900L, request));
            assertEquals(ErrorCode.NOT_OWNER, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("admin moderation")
    class AdminModerationTests {

        @Test
        void updateAdminProductStatus_PublishPendingReview_Success() {
            ProductWarehouseLot pendingLot = buildLot(7, "9");
            MarketplaceProduct pendingProduct = buildProduct(777L, "pending-product", "Pending Product", new BigDecimal("30000"), pendingLot, 20L);
            pendingProduct.setStatus(MarketplaceProductStatus.PENDING_REVIEW);

            when(marketplaceProductRepository.findById(777L)).thenReturn(Optional.of(pendingProduct));
            when(productWarehouseLotRepository.findById(7)).thenReturn(Optional.of(pendingLot));
            when(marketplaceProductRepository.save(any(MarketplaceProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(marketplaceProductReviewRepository.aggregateRatingsByProductIds(List.of(777L))).thenReturn(List.of());

            MarketplaceProductDetailResponse response = marketplaceService.updateAdminProductStatus(
                    777L,
                    new MarketplaceUpdateProductStatusRequest(MarketplaceProductStatus.ACTIVE));

            assertEquals(MarketplaceProductStatus.ACTIVE, response.status());
            assertNotNull(pendingProduct.getPublishedAt());
        }
    }

    private ProductWarehouseLot buildLot(int id, String onHand) {
        User farmer = User.builder().id(20L).build();
        Farm farm = Farm.builder().id(1).name("Green Farm").user(farmer).build();
        Plot plot = Plot.builder().id(1).farm(farm).plotName("Plot A").build();
        Season season = Season.builder().id(1).seasonName("Spring 2026").plot(plot).build();
        return ProductWarehouseLot.builder()
                .id(id)
                .lotCode("LOT-" + id)
                .productName("Produce " + id)
                .farm(farm)
                .season(season)
                .plot(plot)
                .unit("kg")
                .initialQuantity(new BigDecimal(onHand))
                .onHandQuantity(new BigDecimal(onHand))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .build();
    }

    private MarketplaceProduct buildProduct(
            Long id,
            String slug,
            String name,
            BigDecimal price,
            ProductWarehouseLot lot,
            Long farmerId) {
        return MarketplaceProduct.builder()
                .id(id)
                .slug(slug)
                .name(name)
                .price(price)
                .unit("kg")
                .stockQuantity(lot.getOnHandQuantity())
                .status(MarketplaceProductStatus.PUBLISHED)
                .traceable(false)
                .lot(lot)
                .farm(lot.getFarm())
                .season(lot.getSeason())
                .farmerUser(User.builder().id(farmerId).username("farmer-" + farmerId).build())
                .build();
    }

    private void stubCartProduct(MarketplaceProduct product) {
        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(marketplaceCartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(marketplaceProductRepository.findByIdWithLotForCartValidation(product.getId())).thenReturn(Optional.of(product));
    }

    private MarketplaceCartItem cartItem(MarketplaceProduct product, BigDecimal quantity) {
        return MarketplaceCartItem.builder()
                .id(1001L)
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .unitPriceSnapshot(product.getPrice())
                .build();
    }

    private MarketplaceOrder buildOrder(
            Long id,
            String orderCode,
            String groupCode,
            User buyer,
            User farmer,
            MarketplaceProduct product,
            MarketplaceOrderStatus status,
            Long itemId) {
        MarketplaceOrder order = MarketplaceOrder.builder()
                .id(id)
                .orderGroup(MarketplaceOrderGroup.builder().id(id + 1000).groupCode(groupCode).buyerUser(buyer).build())
                .orderCode(orderCode)
                .buyerUser(buyer)
                .farmerUser(farmer)
                .status(status)
                .paymentMethod(MarketplacePaymentMethod.COD)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.NOT_REQUIRED)
                .shippingRecipientName("Buyer")
                .shippingPhone("0909000000")
                .shippingAddressLine("123 Road")
                .subtotal(new BigDecimal("200000"))
                .shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("220000"))
                .build();
        MarketplaceOrderItem item = MarketplaceOrderItem.builder()
                .id(itemId)
                .order(order)
                .product(product)
                .productNameSnapshot(product.getName())
                .productSlugSnapshot(product.getSlug())
                .imageUrlSnapshot("https://example.com/" + product.getSlug() + ".jpg")
                .unitPriceSnapshot(product.getPrice())
                .quantity(new BigDecimal("2"))
                .lineTotal(new BigDecimal("200000"))
                .traceableSnapshot(false)
                .build();
        order.setItems(List.of(item));
        return order;
    }
}
