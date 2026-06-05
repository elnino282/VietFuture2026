package org.example.QuanLyMuaVu.module.marketplace.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.entity.AuditLog;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddressUpsertRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateOrderRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceCreateReviewRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateReviewRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceFarmerProductUpsertRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceMergeCartRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceRejectPaymentProofRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateOrderStatusRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdatePaymentVerificationRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateProductStatusRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateCartItemRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceAdminStatsResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceAddressResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCartItemResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCartResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCartSellerGroupResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceCreateOrderResultResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmerDashboardResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmerProductFormFarmOptionResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmerProductFormLotOptionResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmerProductFormOptionsResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmerProductFormSeasonOptionResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceFarmSummaryResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderAuditLogResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderItemResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderPaymentResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderPreviewResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplacePaymentProofResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceOrderResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductSummaryResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceReviewResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceTraceabilityResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceAddress;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MarketplaceService {

    static BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("20000");
    static String CURRENCY_VND = "VND";
    static BigDecimal LOW_STOCK_THRESHOLD = new BigDecimal("10");
    static BigDecimal ZERO_QUANTITY = BigDecimal.ZERO;
    static String AUDIT_ENTITY_ORDER = "MARKETPLACE_ORDER";
    @SuppressWarnings("deprecation")
    static final List<MarketplaceProductStatus> SELLABLE_PRODUCT_STATUSES = List.of(
            MarketplaceProductStatus.ACTIVE,
            MarketplaceProductStatus.PUBLISHED);
    static final List<MarketplaceProductStatus> HIDDEN_PRODUCT_STATUSES = List.of(
            MarketplaceProductStatus.INACTIVE,
            MarketplaceProductStatus.HIDDEN);

    MarketplaceProductRepository marketplaceProductRepository;
    MarketplaceCartRepository marketplaceCartRepository;
    MarketplaceCartItemRepository marketplaceCartItemRepository;
    MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;
    MarketplaceOrderItemRepository marketplaceOrderItemRepository;
    MarketplaceOrderRepository marketplaceOrderRepository;
    MarketplaceAddressRepository marketplaceAddressRepository;
    MarketplaceProductReviewRepository marketplaceProductReviewRepository;
    FarmRepository farmRepository;
    SeasonRepository seasonRepository;
    ProductWarehouseLotRepository productWarehouseLotRepository;
    ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    CurrentUserService currentUserService;
    ObjectMapper objectMapper;
    AppProperties appProperties;
    AuditLogService auditLogService;
    NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceProductSummaryResponse> listProducts(
            String category,
            String q,
            String region,
            Boolean traceable,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveProductSort(sort));
        Page<MarketplaceProduct> productPage = marketplaceProductRepository.searchPublished(
                buyerVisibleProductStatuses(),
                normalizeNullable(category),
                normalizeNullable(q),
                traceable,
                normalizeNullable(region),
                minPrice,
                maxPrice,
                pageable);

        Map<Long, MarketplaceProductReviewRepository.ProductRatingProjection> ratings = aggregateProductRatings(
                productPage.getContent().stream().map(MarketplaceProduct::getId).toList());

        List<MarketplaceProductSummaryResponse> items = productPage.getContent().stream()
                .map(product -> toProductSummary(product, ratings.get(product.getId())))
                .toList();

        return PageResponse.of(productPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceProductDetailResponse getProductBySlug(String slug) {
        MarketplaceProduct product = marketplaceProductRepository
                .findSellableBySlugAndStatusIn(slug, buyerVisibleProductStatuses())
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));

        validateTraceabilityChain(product);

        MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(product.getId()))
                .get(product.getId());

        return toProductDetail(product, rating);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceReviewResponse> listProductReviews(Long productId, int page, int size) {
        marketplaceProductRepository.findSellableByIdAndStatusIn(productId, buyerVisibleProductStatuses())
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MarketplaceProductReview> reviewPage = marketplaceProductReviewRepository.findVisibleByProductId(productId, pageable);
        List<MarketplaceReviewResponse> items = reviewPage.getContent().stream()
                .map(this::toReviewResponse)
                .toList();

        return PageResponse.of(reviewPage, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceReviewResponse> listFarmReviews(Integer farmId, int page, int size) {
        farmRepository.findById(farmId).orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<MarketplaceProductReview> reviewPage = marketplaceProductReviewRepository.findVisibleByFarmId(farmId, pageable);
        List<MarketplaceReviewResponse> items = reviewPage.getContent().stream()
                .map(this::toReviewResponse)
                .toList();

        return PageResponse.of(reviewPage, items);
    }

    // ─────────────── Review CRUD (Buyer) ───────────────

    @Transactional
    public MarketplaceReviewResponse createReview(Long orderId, MarketplaceCreateReviewRequest request) {
        User buyer = currentUserService.getCurrentUser();
        Long buyerUserId = buyer.getId();

        // 1. Validate order exists and belongs to buyer
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));

        // 2. Validate order is COMPLETED
        if (order.getStatus() != MarketplaceOrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.MARKETPLACE_REVIEW_ORDER_NOT_COMPLETED);
        }

        // 3. Validate orderItem belongs to this order
        MarketplaceOrderItem orderItem = marketplaceOrderItemRepository.findByIdAndOrder_Id(request.orderItemId(), orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER));

        // 4. Validate no duplicate review
        if (marketplaceProductReviewRepository.existsByOrderItem_IdAndBuyerUser_Id(orderItem.getId(), buyerUserId)) {
            throw new AppException(ErrorCode.MARKETPLACE_REVIEW_ALREADY_EXISTS);
        }

        // 5. Validate rating range (belt-and-suspenders, DTO already validates)
        if (request.rating() < 1 || request.rating() > 5) {
            throw new AppException(ErrorCode.MARKETPLACE_INVALID_RATING);
        }

        MarketplaceProduct product = orderItem.getProduct();

        MarketplaceProductReview review = MarketplaceProductReview.builder()
                .product(product)
                .order(order)
                .orderItem(orderItem)
                .buyerUser(buyer)
                .rating(request.rating())
                .comment(normalizeNullable(request.comment()))
                .hidden(false)
                .build();

        review = marketplaceProductReviewRepository.save(review);

        // Recalculate denormalized ratings
        recalculateProductRating(product.getId());
        if (product.getFarm() != null) {
            recalculateFarmRating(product.getFarm().getId());
        }

        return toReviewResponse(review);
    }

    /**
     * Deprecated-compatible create review — called from the old POST /marketplace/reviews endpoint.
     * Requires productId and orderId in the request body instead of orderId as path variable.
     */
    @Transactional
    public MarketplaceReviewResponse createReviewLegacy(MarketplaceCreateReviewRequest request) {
        // For legacy endpoint, orderItemId is still used.
        // We need to find the order from the orderItem.
        MarketplaceOrderItem orderItem = marketplaceOrderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER));
        return createReview(orderItem.getOrder().getId(), request);
    }

    @Transactional
    public MarketplaceReviewResponse editReview(Long reviewId, MarketplaceUpdateReviewRequest request) {
        Long buyerUserId = currentUserService.getCurrentUserId();

        MarketplaceProductReview review = marketplaceProductReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND));

        // Ownership check
        if (!Objects.equals(review.getBuyerUser().getId(), buyerUserId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        boolean ratingChanged = false;
        if (request.rating() != null) {
            if (request.rating() < 1 || request.rating() > 5) {
                throw new AppException(ErrorCode.MARKETPLACE_INVALID_RATING);
            }
            if (!Objects.equals(review.getRating(), request.rating())) {
                review.setRating(request.rating());
                ratingChanged = true;
            }
        }
        // comment can be set to null (clear) or updated
        review.setComment(normalizeNullable(request.comment()));

        review = marketplaceProductReviewRepository.save(review);

        if (ratingChanged) {
            recalculateProductRating(review.getProduct().getId());
            if (review.getProduct().getFarm() != null) {
                recalculateFarmRating(review.getProduct().getFarm().getId());
            }
        }

        return toReviewResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Long buyerUserId = currentUserService.getCurrentUserId();

        MarketplaceProductReview review = marketplaceProductReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND));

        // Ownership check
        if (!Objects.equals(review.getBuyerUser().getId(), buyerUserId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        Long productId = review.getProduct().getId();
        Integer farmId = review.getProduct().getFarm() != null ? review.getProduct().getFarm().getId() : null;

        marketplaceProductReviewRepository.delete(review);

        recalculateProductRating(productId);
        if (farmId != null) {
            recalculateFarmRating(farmId);
        }
    }

    // ─────────────── Review Admin ───────────────

    @Transactional
    public MarketplaceReviewResponse adminHideReview(Long reviewId) {
        MarketplaceProductReview review = marketplaceProductReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND));

        review.setHidden(true);
        review = marketplaceProductReviewRepository.save(review);

        recalculateProductRating(review.getProduct().getId());
        if (review.getProduct().getFarm() != null) {
            recalculateFarmRating(review.getProduct().getFarm().getId());
        }

        return toReviewResponse(review);
    }

    @Transactional
    public void adminDeleteReview(Long reviewId) {
        MarketplaceProductReview review = marketplaceProductReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_NOT_FOUND));

        Long productId = review.getProduct().getId();
        Integer farmId = review.getProduct().getFarm() != null ? review.getProduct().getFarm().getId() : null;

        marketplaceProductReviewRepository.delete(review);

        recalculateProductRating(productId);
        if (farmId != null) {
            recalculateFarmRating(farmId);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceFarmSummaryResponse> listFarms(String q, String region, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Farm> farmPage = marketplaceProductRepository.searchDistinctFarmsWithPublishedProducts(
                buyerVisibleProductStatuses(),
                normalizeNullable(q),
                normalizeNullable(region),
                pageable);

        List<Integer> farmIds = farmPage.getContent().stream().map(Farm::getId).toList();
        Map<Integer, Long> productCountByFarmId = aggregateFarmProductCounts(farmIds);

        List<MarketplaceFarmSummaryResponse> items = farmPage.getContent().stream()
                .map(farm -> toFarmSummary(farm, productCountByFarmId.getOrDefault(farm.getId(), 0L)))
                .toList();

        return PageResponse.of(farmPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceFarmDetailResponse getFarmDetail(Integer farmId) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));
        long productCount = marketplaceProductRepository.countSellableByFarmIdAndStatusIn(
                farmId,
                buyerVisibleProductStatuses());

        MarketplaceFarmSummaryResponse summary = toFarmSummary(farm, productCount);
        User owner = farm.getUser();
        String ownerName = owner == null ? null : defaultDisplayName(owner);
        String ownerPhone = owner == null ? null : owner.getPhone();

        return new MarketplaceFarmDetailResponse(
                summary.id(),
                summary.name(),
                summary.region(),
                summary.address(),
                summary.coverImageUrl(),
                summary.productCount(),
                null,
                owner == null ? null : owner.getId(),
                ownerName,
                ownerPhone);
    }

    @Transactional(readOnly = true)
    public MarketplaceTraceabilityResponse getTraceability(Long productId) {
        MarketplaceProduct product = marketplaceProductRepository.findSellableByIdAndStatusIn(
                        productId,
                        buyerVisibleProductStatuses())
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));

        return buildTraceabilityResponse(
                product.getId(),
                Boolean.TRUE.equals(product.getTraceable()),
                product.getFarm(),
                product.getSeason(),
                product.getLot(),
                product.getPublishedAt());
    }

    @Transactional(readOnly = true)
    public MarketplaceTraceabilityResponse getOrderItemTraceability(Long orderId, Long itemId) {
        Long buyerUserId = currentUserService.getCurrentUserId();

        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));

        MarketplaceOrderItem orderItem = order.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER));

        Long productId = orderItem.getProduct() != null ? orderItem.getProduct().getId() : null;
        boolean traceable = Boolean.TRUE.equals(orderItem.getTraceableSnapshot());

        // Use snapshotted references from order item (they are captured at order creation time)
        LocalDateTime publishedAt = orderItem.getProduct() != null
                ? orderItem.getProduct().getPublishedAt() : null;

        return buildTraceabilityResponse(
                productId,
                traceable,
                orderItem.getFarm(),
                orderItem.getSeason(),
                orderItem.getLot(),
                publishedAt);
    }

    private MarketplaceTraceabilityResponse buildTraceabilityResponse(
            Long productId,
            boolean traceable,
            Farm farm,
            Season season,
            ProductWarehouseLot lot,
            LocalDateTime publishedAt) {

        // Farm — graceful null fallback
        MarketplaceTraceabilityResponse.FarmTraceability farmTrace = null;
        if (farm != null) {
            farmTrace = new MarketplaceTraceabilityResponse.FarmTraceability(
                    farm.getId(),
                    farm.getName(),
                    resolveFarmRegion(farm),
                    resolveFarmAddress(farm),
                    null); // TODO: certificationInfo — pending feature scoping
        }

        // Plot — navigated via lot.plot (primary source) or season.plot (fallback)
        MarketplaceTraceabilityResponse.PlotTraceability plotTrace = null;
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = null;
        if (lot != null && lot.getPlot() != null) {
            plot = lot.getPlot();
        } else if (season != null && season.getPlot() != null) {
            plot = season.getPlot();
        }
        if (plot != null) {
            plotTrace = new MarketplaceTraceabilityResponse.PlotTraceability(
                    plot.getId(),
                    plot.getPlotName(),
                    plot.getArea());
        }

        // Season — includes crop name via season.crop
        MarketplaceTraceabilityResponse.SeasonTraceability seasonTrace = null;
        if (season != null) {
            String cropName = season.getCrop() != null ? season.getCrop().getCropName() : null;
            String varietyName = season.getVariety() != null ? season.getVariety().getName() : null;
            seasonTrace = new MarketplaceTraceabilityResponse.SeasonTraceability(
                    season.getId(),
                    season.getSeasonName(),
                    cropName,
                    varietyName,
                    season.getStartDate(),
                    season.getPlannedHarvestDate());
        }

        // Harvest — from lot.harvest
        MarketplaceTraceabilityResponse.HarvestTraceability harvestTrace = null;
        if (lot != null && lot.getHarvest() != null) {
            var harvest = lot.getHarvest();
            harvestTrace = new MarketplaceTraceabilityResponse.HarvestTraceability(
                    harvest.getId(),
                    harvest.getHarvestDate(),
                    harvest.getQuantity(),
                    harvest.getGrade()); // grade as quality notes — never expose internal 'note'
        }

        // Product lot — lot details + warehouse/storage info
        MarketplaceTraceabilityResponse.ProductLotTraceability lotTrace = null;
        if (lot != null) {
            String warehouseName = lot.getWarehouse() != null ? lot.getWarehouse().getName() : null;
            String storageLocation = buildStorageLocation(lot.getLocation());
            lotTrace = new MarketplaceTraceabilityResponse.ProductLotTraceability(
                    lot.getId(),
                    lot.getLotCode(),
                    lot.getHarvestedAt(),
                    lot.getReceivedAt(),
                    lot.getUnit(),
                    lot.getInitialQuantity(),
                    lot.getGrade(),
                    warehouseName,
                    storageLocation);
        }

        // Timeline milestones — derived from entity state
        List<MarketplaceTraceabilityResponse.TimelineMilestone> timeline =
                buildTimelineMilestones(season, lot, publishedAt);

        return new MarketplaceTraceabilityResponse(
                productId,
                traceable,
                farmTrace,
                plotTrace,
                seasonTrace,
                harvestTrace,
                lotTrace,
                timeline,
                LocalDateTime.now());
    }

    private List<MarketplaceTraceabilityResponse.TimelineMilestone> buildTimelineMilestones(
            Season season,
            ProductWarehouseLot lot,
            LocalDateTime publishedAt) {
        List<MarketplaceTraceabilityResponse.TimelineMilestone> milestones = new ArrayList<>();

        // PLANTED — season start date
        if (season != null && season.getStartDate() != null) {
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "PLANTED",
                    season.getStartDate().atStartOfDay(),
                    "Crop planted — " + (season.getCrop() != null ? season.getCrop().getCropName() : season.getSeasonName())));
        }

        // TENDED — season is ACTIVE, COMPLETED, or ARCHIVED
        if (season != null && season.getStatus() != null
                && season.getStatus() != org.example.QuanLyMuaVu.Enums.SeasonStatus.PLANNED
                && season.getStatus() != org.example.QuanLyMuaVu.Enums.SeasonStatus.CANCELLED) {
            LocalDateTime tendedAt = season.getStartDate() != null
                    ? season.getStartDate().plusDays(1).atStartOfDay() : null;
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "TENDED",
                    tendedAt,
                    "Crop tended during growing season"));
        }

        // HARVESTED — from lot.harvest or lot.harvestedAt
        if (lot != null && lot.getHarvest() != null && lot.getHarvest().getHarvestDate() != null) {
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "HARVESTED",
                    lot.getHarvest().getHarvestDate().atStartOfDay(),
                    "Crop harvested — grade: " + (lot.getHarvest().getGrade() != null ? lot.getHarvest().getGrade() : "N/A")));
        } else if (lot != null && lot.getHarvestedAt() != null) {
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "HARVESTED",
                    lot.getHarvestedAt().atStartOfDay(),
                    "Crop harvested"));
        }

        // STORED — lot received at warehouse
        if (lot != null && lot.getReceivedAt() != null) {
            String warehouseName = lot.getWarehouse() != null ? lot.getWarehouse().getName() : "warehouse";
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "STORED",
                    lot.getReceivedAt(),
                    "Stored in " + warehouseName));
        }

        // LISTED — product published on marketplace
        if (publishedAt != null) {
            milestones.add(new MarketplaceTraceabilityResponse.TimelineMilestone(
                    "LISTED",
                    publishedAt,
                    "Listed on marketplace"));
        }

        // Sort chronologically (nulls last)
        milestones.sort(Comparator.comparing(
                MarketplaceTraceabilityResponse.TimelineMilestone::date,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return milestones;
    }

    private String buildStorageLocation(org.example.QuanLyMuaVu.module.inventory.entity.StockLocation location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (location.getZone() != null) {
            sb.append("Zone ").append(location.getZone());
        }
        if (location.getAisle() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Aisle ").append(location.getAisle());
        }
        if (location.getShelf() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Shelf ").append(location.getShelf());
        }
        if (location.getBin() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Bin ").append(location.getBin());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    @Transactional(readOnly = true)
    public MarketplaceCartResponse getCart() {
        Long userId = currentUserService.getCurrentUserId();
        Optional<MarketplaceCart> cartOpt = marketplaceCartRepository.findByUser_Id(userId);
        if (cartOpt.isEmpty()) {
            return emptyCart(userId);
        }
        return buildCartResponse(userId, cartOpt.get());
    }

    @Transactional
    public MarketplaceCartResponse addCartItem(MarketplaceAddCartItemRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        MarketplaceCart cart = getOrCreateCartForUpdate(currentUser);
        validatePositiveQuantity(request.quantity());
        MarketplaceProduct product = getCartProductOrThrow(request.productId(), request.quantity(), request.quantity());

        MarketplaceCartItem item = marketplaceCartItemRepository
                .findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                .orElse(null);
        BigDecimal targetQuantity = request.quantity();
        if (item != null) {
            targetQuantity = item.getQuantity().add(request.quantity());
        }

        validateCartProductAvailability(product, request.quantity(), targetQuantity);

        if (item == null) {
            item = MarketplaceCartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPriceSnapshot(product.getPrice())
                    .build();
        } else {
            item.setQuantity(targetQuantity);
            item.setUnitPriceSnapshot(product.getPrice());
        }

        marketplaceCartItemRepository.save(item);
        return buildCartResponse(currentUser.getId(), cart);
    }

    @Transactional
    public MarketplaceCartResponse updateCartItem(Long productId, MarketplaceUpdateCartItemRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceCart cart = marketplaceCartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_CART_ITEM_NOT_FOUND));

        validatePositiveQuantity(request.quantity());
        MarketplaceProduct product = getCartProductOrThrow(productId, request.quantity(), request.quantity());
        validateCartProductAvailability(product, request.quantity(), request.quantity());

        MarketplaceCartItem item = marketplaceCartItemRepository
                .findByCart_IdAndProduct_Id(cart.getId(), productId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_CART_ITEM_NOT_FOUND));
        item.setQuantity(request.quantity());
        item.setUnitPriceSnapshot(product.getPrice());
        marketplaceCartItemRepository.save(item);

        return buildCartResponse(userId, cart);
    }

    @Transactional
    public MarketplaceCartResponse removeCartItem(Long productId) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceCart cart = marketplaceCartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_CART_ITEM_NOT_FOUND));
        int deleted = marketplaceCartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
        if (deleted == 0) {
            throw new AppException(ErrorCode.MARKETPLACE_CART_ITEM_NOT_FOUND);
        }
        return buildCartResponse(userId, cart);
    }

    @Transactional
    public MarketplaceCartResponse clearCart() {
        Long userId = currentUserService.getCurrentUserId();
        Optional<MarketplaceCart> cartOpt = marketplaceCartRepository.findByUserIdForUpdate(userId);
        if (cartOpt.isEmpty()) {
            return emptyCart(userId);
        }
        MarketplaceCart cart = cartOpt.get();
        marketplaceCartItemRepository.deleteAllByCartId(cart.getId());
        return buildCartResponse(userId, cart);
    }

    @Transactional
    public MarketplaceCartResponse mergeCart(MarketplaceMergeCartRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        MarketplaceCart cart = getOrCreateCartForUpdate(currentUser);

        Map<Long, BigDecimal> incomingByProductId = new LinkedHashMap<>();
        for (MarketplaceMergeCartRequest.MarketplaceMergeCartItem item : request.items()) {
            validatePositiveQuantity(item.quantity());
            incomingByProductId.merge(item.productId(), item.quantity(), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : incomingByProductId.entrySet()) {
            MarketplaceProduct product = getCartProductOrThrow(entry.getKey(), entry.getValue(), entry.getValue());
            MarketplaceCartItem existing = marketplaceCartItemRepository
                    .findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                    .orElse(null);

            BigDecimal mergedQuantity = entry.getValue();
            if (existing != null) {
                mergedQuantity = existing.getQuantity().add(entry.getValue());
            }
            validateCartProductAvailability(product, entry.getValue(), mergedQuantity);

            if (existing == null) {
                existing = MarketplaceCartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(entry.getValue())
                        .unitPriceSnapshot(product.getPrice())
                        .build();
            } else {
                existing.setQuantity(mergedQuantity);
                existing.setUnitPriceSnapshot(product.getPrice());
            }
            marketplaceCartItemRepository.save(existing);
        }

        return buildCartResponse(currentUser.getId(), cart);
    }

    @Transactional
    public MarketplaceCreateOrderResultResponse createOrder(MarketplaceCreateOrderRequest request, String headerIdempotencyKey) {
        User buyer = currentUserService.getCurrentUser();
        Long buyerUserId = buyer.getId();
        String idempotencyKey = resolveIdempotencyKey(headerIdempotencyKey, request.idempotencyKey());
        if (idempotencyKey == null) {
            throw new AppException(ErrorCode.MARKETPLACE_IDEMPOTENCY_KEY_REQUIRED);
        }

        MarketplaceCart cart = marketplaceCartRepository.findByUserIdForUpdate(buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_CART_EMPTY));
        List<MarketplaceCartItem> cartItems = marketplaceCartItemRepository.findByCartIdWithProductForUpdate(cart.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.MARKETPLACE_CART_EMPTY);
        }

        String requestFingerprint = buildOrderFingerprint(request, cartItems);

        Optional<MarketplaceOrderGroup> existingGroupOpt = marketplaceOrderGroupRepository
                .findByBuyerUser_IdAndIdempotencyKey(buyerUserId, idempotencyKey);
        if (existingGroupOpt.isPresent()) {
            MarketplaceOrderGroup existingGroup = existingGroupOpt.get();
            if (!Objects.equals(existingGroup.getRequestFingerprint(), requestFingerprint)) {
                throw new AppException(ErrorCode.MARKETPLACE_IDEMPOTENCY_CONFLICT);
            }
            return buildOrderResultForExistingGroup(existingGroup.getId(), existingGroup.getGroupCode());
        }

        Collection<Long> productIds = cartItems.stream()
                .map(ci -> ci.getProduct().getId())
                .collect(Collectors.toCollection(ArrayList::new));
        List<MarketplaceProduct> lockedProducts = marketplaceProductRepository.findAllByIdInForUpdate(productIds);
        Map<Long, MarketplaceProduct> lockedProductById = lockedProducts.stream()
                .collect(Collectors.toMap(MarketplaceProduct::getId, p -> p));

        if (lockedProductById.size() != productIds.size()) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND);
        }

        List<Integer> lotIds = lockedProducts.stream()
                .map(MarketplaceProduct::getLot)
                .filter(Objects::nonNull)
                .map(ProductWarehouseLot::getId)
                .distinct()
                .toList();
        List<ProductWarehouseLot> lockedLots = productWarehouseLotRepository.findAllByIdInForUpdate(lotIds);
        Map<Integer, ProductWarehouseLot> lockedLotById = lockedLots.stream()
                .collect(Collectors.toMap(ProductWarehouseLot::getId, lot -> lot));

        for (MarketplaceCartItem item : cartItems) {
            MarketplaceProduct lockedProduct = lockedProductById.get(item.getProduct().getId());
            if (lockedProduct == null) {
                throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND);
            }
            if (lockedProduct.getStatus() != MarketplaceProductStatus.PUBLISHED
                    && lockedProduct.getStatus() != MarketplaceProductStatus.ACTIVE) {
                throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_PUBLISHED);
            }
            validateTraceabilityChain(lockedProduct);

            ProductWarehouseLot lot = resolveLockedLot(lockedProduct, lockedLotById);
            ensureLotSellable(lot);
            ensureStockAvailable(lockedProduct, item.getQuantity());
        }

        ShippingSnapshot shippingSnapshot = resolveShippingSnapshot(buyerUserId, request);

        MarketplaceOrderGroup orderGroup = MarketplaceOrderGroup.builder()
                .groupCode(generateOrderGroupCode())
                .buyerUser(buyer)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .build();

        try {
            orderGroup = marketplaceOrderGroupRepository.save(orderGroup);
            marketplaceOrderGroupRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            MarketplaceOrderGroup existingGroup = marketplaceOrderGroupRepository
                    .findByBuyerUser_IdAndIdempotencyKey(buyerUserId, idempotencyKey)
                    .orElseThrow(() -> ex);
            if (!Objects.equals(existingGroup.getRequestFingerprint(), requestFingerprint)) {
                throw new AppException(ErrorCode.MARKETPLACE_IDEMPOTENCY_CONFLICT);
            }
            return buildOrderResultForExistingGroup(existingGroup.getId(), existingGroup.getGroupCode());
        }

        Map<Long, List<MarketplaceCartItem>> groupedByFarmer = cartItems.stream()
                .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                .collect(Collectors.groupingBy(
                        ci -> ci.getProduct().getFarmerUser().getId(),
                        TreeMap::new,
                        Collectors.toList()));

        List<MarketplaceOrder> createdOrders = new ArrayList<>();
        for (Map.Entry<Long, List<MarketplaceCartItem>> groupEntry : groupedByFarmer.entrySet()) {
            Long farmerUserId = groupEntry.getKey();
            List<MarketplaceCartItem> groupItems = groupEntry.getValue();
            User farmer = groupItems.getFirst().getProduct().getFarmerUser();

            BigDecimal subtotal = BigDecimal.ZERO;
            List<MarketplaceOrderItem> orderItems = new ArrayList<>();
            for (MarketplaceCartItem cartItem : groupItems) {
                MarketplaceProduct lockedProduct = lockedProductById.get(cartItem.getProduct().getId());
                ProductWarehouseLot lockedLot = resolveLockedLot(lockedProduct, lockedLotById);
                BigDecimal lineTotal = lockedProduct.getPrice().multiply(cartItem.getQuantity());
                subtotal = subtotal.add(lineTotal);

                MarketplaceOrderItem orderItem = MarketplaceOrderItem.builder()
                        .product(lockedProduct)
                        .productNameSnapshot(lockedProduct.getName())
                        .productSlugSnapshot(lockedProduct.getSlug())
                        .imageUrlSnapshot(lockedProduct.getImageUrl())
                        .unitPriceSnapshot(lockedProduct.getPrice())
                        .quantity(cartItem.getQuantity())
                        .lineTotal(lineTotal)
                        .traceableSnapshot(Boolean.TRUE.equals(lockedProduct.getTraceable()))
                        .farm(lockedProduct.getFarm())
                        .season(lockedProduct.getSeason())
                        .lot(lockedLot)
                        .build();
                orderItems.add(orderItem);

                lockedProduct.setStockQuantity(currentListingQuantity(lockedProduct).subtract(cartItem.getQuantity()));
                lockedLot.setOnHandQuantity(lockedLot.getOnHandQuantity().subtract(cartItem.getQuantity()));
                productWarehouseTransactionRepository.save(buildMarketplaceLotTransaction(
                        lockedLot,
                        ProductWarehouseTransactionType.MARKETPLACE_ORDER_RESERVED,
                        cartItem.getQuantity().negate(),
                        "ORDER",
                        orderGroup.getGroupCode(),
                        "Reserved for marketplace checkout",
                        buyer));
            }

            BigDecimal shippingFee = DEFAULT_SHIPPING_FEE;
            MarketplaceOrder order = MarketplaceOrder.builder()
                    .orderGroup(orderGroup)
                    .orderCode(generateOrderCode())
                    .buyerUser(buyer)
                    .farmerUser(farmer)
                    .status(MarketplaceOrderStatus.PENDING_PAYMENT)
                    .paymentMethod(request.paymentMethod())
                    .paymentVerificationStatus(resolveInitialPaymentVerificationStatus(request.paymentMethod()))
                    .shippingRecipientName(shippingSnapshot.recipientName())
                    .shippingPhone(shippingSnapshot.phone())
                    .shippingAddressLine(shippingSnapshot.addressLine())
                    .note(normalizeNullable(request.note()))
                    .subtotal(subtotal)
                    .shippingFee(shippingFee)
                    .totalAmount(subtotal.add(shippingFee))
                    .items(new ArrayList<>())
                    .build();

            for (MarketplaceOrderItem item : orderItems) {
                item.setOrder(order);
                order.getItems().add(item);
            }

            createdOrders.add(marketplaceOrderRepository.save(order));
            log.debug("Created marketplace order for farmer {} with {} items", farmerUserId, orderItems.size());
        }

        marketplaceProductRepository.saveAll(lockedProducts);
        productWarehouseLotRepository.saveAll(lockedLots);
        marketplaceCartItemRepository.deleteAllByCartId(cart.getId());

        // COD auto-advance: immediately move through PAYMENT_SUBMITTED → PAYMENT_VERIFIED
        if (request.paymentMethod() == MarketplacePaymentMethod.COD) {
            for (MarketplaceOrder codOrder : createdOrders) {
                codOrder.setStatus(MarketplaceOrderStatus.PAYMENT_SUBMITTED);
                codOrder.setStatus(MarketplaceOrderStatus.PAYMENT_VERIFIED);
                marketplaceOrderRepository.save(codOrder);
            }
        }

        for (MarketplaceOrder createdOrder : createdOrders) {
            notifyUser(
                    buyerUserId,
                    "Order created",
                    "Your order " + createdOrder.getOrderCode() + " has been created.",
                    "/marketplace/orders/" + createdOrder.getId());
            if (createdOrder.getFarmerUser() != null) {
                notifyUser(
                        createdOrder.getFarmerUser().getId(),
                        "New marketplace order",
                        "New order " + createdOrder.getOrderCode() + " is waiting for processing.",
                        "/farmer/marketplace-orders/" + createdOrder.getId());
            }
            auditOrderOperation(createdOrder, "ORDER_CREATED", "Marketplace order created");
        }

        List<MarketplaceOrderResponse> orderResponses = createdOrders.stream()
                .map(this::toOrderResponse)
                .toList();

        return new MarketplaceCreateOrderResultResponse(
                orderGroup.getGroupCode(),
                orderResponses.size(),
                orderResponses);
    }

    /**
     * Preview checkout — reads cart, groups by seller, calculates totals,
     * resolves shipping address, but does NOT create any DB records or modify stock.
     */
    @Transactional(readOnly = true)
    public MarketplaceOrderPreviewResponse previewOrder(MarketplaceCreateOrderRequest request) {
        User buyer = currentUserService.getCurrentUser();
        Long buyerUserId = buyer.getId();

        MarketplaceCart cart = marketplaceCartRepository.findByUserIdForUpdate(buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_CART_EMPTY));
        List<MarketplaceCartItem> cartItems = marketplaceCartItemRepository.findByCartIdWithProduct(cart.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.MARKETPLACE_CART_EMPTY);
        }

        ShippingSnapshot shippingSnapshot = resolveShippingSnapshot(buyerUserId, request);

        // Group by farmer
        Map<Long, List<MarketplaceCartItem>> groupedByFarmer = cartItems.stream()
                .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                .collect(Collectors.groupingBy(
                        ci -> ci.getProduct().getFarmerUser().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        BigDecimal grandSubtotal = BigDecimal.ZERO;
        BigDecimal grandShippingFee = BigDecimal.ZERO;
        List<MarketplaceOrderPreviewResponse.SellerGroup> sellerGroups = new ArrayList<>();

        for (Map.Entry<Long, List<MarketplaceCartItem>> entry : groupedByFarmer.entrySet()) {
            List<MarketplaceCartItem> sellerItems = entry.getValue();
            MarketplaceProduct firstProduct = sellerItems.getFirst().getProduct();
            User farmer = firstProduct.getFarmerUser();
            Farm farm = firstProduct.getFarm();

            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            List<MarketplaceOrderPreviewResponse.PreviewItem> previewItems = new ArrayList<>();

            for (MarketplaceCartItem item : sellerItems) {
                MarketplaceProduct product = item.getProduct();
                BigDecimal unitPrice = product.getPrice();
                BigDecimal lineTotal = unitPrice.multiply(item.getQuantity());
                sellerSubtotal = sellerSubtotal.add(lineTotal);

                previewItems.add(new MarketplaceOrderPreviewResponse.PreviewItem(
                        product.getId(),
                        product.getSlug(),
                        product.getName(),
                        product.getImageUrl(),
                        unitPrice,
                        item.getQuantity(),
                        lineTotal,
                        Boolean.TRUE.equals(product.getTraceable())));
            }

            BigDecimal groupShipping = DEFAULT_SHIPPING_FEE;
            grandSubtotal = grandSubtotal.add(sellerSubtotal);
            grandShippingFee = grandShippingFee.add(groupShipping);

            sellerGroups.add(new MarketplaceOrderPreviewResponse.SellerGroup(
                    farmer.getId(),
                    defaultDisplayName(farmer),
                    farm == null ? null : farm.getId(),
                    farm == null ? null : farm.getName(),
                    previewItems,
                    sellerSubtotal,
                    groupShipping,
                    sellerSubtotal.add(groupShipping)));
        }

        return new MarketplaceOrderPreviewResponse(
                sellerGroups,
                grandSubtotal,
                grandShippingFee,
                grandSubtotal.add(grandShippingFee),
                shippingSnapshot.recipientName(),
                shippingSnapshot.phone(),
                shippingSnapshot.addressLine(),
                sellerGroups.size(),
                CURRENCY_VND);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceOrderResponse> listOrders(String status, int page, int size) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceOrderStatus normalizedStatus = normalizeBuyerOrderStatus(status);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Long> orderIdPage = normalizedStatus == null
                ? marketplaceOrderRepository.findBuyerOrderIds(userId, pageable)
                : marketplaceOrderRepository.findBuyerOrderIdsByStatus(userId, normalizedStatus, pageable);

        List<Long> orderIds = orderIdPage.getContent();
        if (orderIds.isEmpty()) {
            return PageResponse.of(orderIdPage, List.of());
        }

        Map<Long, MarketplaceOrder> hydratedById = marketplaceOrderRepository.findByIdsWithResponseGraph(orderIds).stream()
                .collect(Collectors.toMap(MarketplaceOrder::getId, order -> order, (left, right) -> left));
        Map<Long, Map<Long, Long>> reviewIdsByOrderAndProduct = loadReviewIdsByOrderAndProduct(orderIds, userId);

        List<MarketplaceOrderResponse> items = orderIds.stream()
                .map(orderId -> Optional.ofNullable(hydratedById.get(orderId))
                        .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND)))
                .map(order -> toOrderResponse(
                        order,
                        userId,
                        reviewIdsByOrderAndProduct.getOrDefault(order.getId(), Collections.emptyMap())))
                .toList();
        return PageResponse.of(orderIdPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceOrderResponse getOrderDetail(Long orderId) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Transactional
    public MarketplaceOrderResponse uploadPaymentProof(Long orderId, MultipartFile file) {
        Long buyerUserId = currentUserService.getCurrentUserId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));

        if (order.getPaymentMethod() != MarketplacePaymentMethod.BANK_TRANSFER
                || (order.getStatus() != MarketplaceOrderStatus.PENDING_PAYMENT
                        && order.getStatus() != MarketplaceOrderStatus.PAYMENT_SUBMITTED)) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_NOT_ALLOWED);
        }
        if (file == null || file.isEmpty() || normalizeNullable(file.getOriginalFilename()) == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_INVALID);
        }

        storePaymentProof(order, file);
        order.setPaymentVerificationStatus(MarketplacePaymentVerificationStatus.SUBMITTED);
        // Advance order status from PENDING_PAYMENT → PAYMENT_SUBMITTED when proof is uploaded
        if (order.getStatus() == MarketplaceOrderStatus.PENDING_PAYMENT) {
            order.setStatus(MarketplaceOrderStatus.PAYMENT_SUBMITTED);
        }
        order.setPaymentProofUploadedAt(LocalDateTime.now());
        order.setPaymentVerifiedAt(null);
        order.setPaymentVerifiedByUserId(null);
        order.setPaymentVerificationNote(null);

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        auditOrderOperation(saved, "PAYMENT_PROOF_SUBMITTED", "Buyer submitted payment proof");

        if (saved.getFarmerUser() != null) {
            notifyUser(
                    saved.getFarmerUser().getId(),
                    "Payment proof submitted",
                    "Buyer submitted transfer proof for order " + saved.getOrderCode() + ".",
                    "/farmer/marketplace-orders/" + saved.getId());
        }
        return toOrderResponse(saved);
    }

    @Transactional
    public MarketplaceOrderResponse cancelOrder(Long orderId) {
        User buyer = currentUserService.getCurrentUser();
        Long buyerUserId = buyer.getId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));

        if (order.getStatus() != MarketplaceOrderStatus.PENDING_PAYMENT
                && order.getStatus() != MarketplaceOrderStatus.PAYMENT_SUBMITTED
                && order.getStatus() != MarketplaceOrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.MARKETPLACE_ORDER_CANCEL_NOT_ALLOWED);
        }

        restoreOrderStock(order, buyer, "Buyer cancelled order");

        order.setStatus(MarketplaceOrderStatus.CANCELLED);
        MarketplaceOrder savedOrder = marketplaceOrderRepository.save(order);
        auditOrderOperation(savedOrder, "ORDER_CANCELLED", "Buyer cancelled order");
        notifyUser(
                buyerUserId,
                "Order cancelled",
                "Your order " + savedOrder.getOrderCode() + " has been cancelled.",
                "/marketplace/orders/" + savedOrder.getId());
        if (savedOrder.getFarmerUser() != null) {
            notifyUser(
                    savedOrder.getFarmerUser().getId(),
                    "Order cancelled",
                    "Buyer cancelled order " + savedOrder.getOrderCode() + ".",
                    "/farmer/marketplace-orders/" + savedOrder.getId());
        }
        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public MarketplacePaymentProofResponse getPaymentProof(Long orderId) {
        Long buyerUserId = currentUserService.getCurrentUserId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        if (normalizeNullable(order.getPaymentProofStoragePath()) == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_REQUIRED);
        }
        return toPaymentProofResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplacePaymentProofResponse> listAdminPaymentProofs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "paymentProofUploadedAt"));
        Page<MarketplaceOrder> orderPage = marketplaceOrderRepository.findByPaymentVerificationStatus(
                MarketplacePaymentVerificationStatus.SUBMITTED, pageable);
        List<MarketplacePaymentProofResponse> items = orderPage.getContent().stream()
                .map(this::toPaymentProofResponse)
                .toList();
        return PageResponse.of(orderPage, items);
    }

    @Transactional
    public MarketplacePaymentProofResponse verifyAdminPaymentProof(Long orderId) {
        MarketplaceOrder order = marketplaceOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        if (order.getPaymentMethod() != MarketplacePaymentMethod.BANK_TRANSFER) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }
        if (normalizeNullable(order.getPaymentProofStoragePath()) == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_REQUIRED);
        }
        if (order.getPaymentVerificationStatus() != MarketplacePaymentVerificationStatus.SUBMITTED) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }

        User admin = currentUserService.getCurrentUser();
        order.setPaymentVerificationStatus(MarketplacePaymentVerificationStatus.VERIFIED);
        order.setPaymentVerifiedAt(LocalDateTime.now());
        order.setPaymentVerifiedByUserId(admin.getId());
        order.setPaymentVerificationNote(null);

        // Auto-advance order status to PAYMENT_VERIFIED
        if (order.getStatus() == MarketplaceOrderStatus.PAYMENT_SUBMITTED) {
            order.setStatus(MarketplaceOrderStatus.PAYMENT_VERIFIED);
        }

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        auditOrderOperation(saved, "PAYMENT_VERIFIED", "Admin verified payment proof");

        if (saved.getBuyerUser() != null) {
            notifyUser(
                    saved.getBuyerUser().getId(),
                    "Payment verified",
                    "Payment for order " + saved.getOrderCode() + " has been verified.",
                    "/marketplace/orders/" + saved.getId());
        }
        if (saved.getFarmerUser() != null) {
            notifyUser(
                    saved.getFarmerUser().getId(),
                    "Payment verified",
                    "Payment for order " + saved.getOrderCode() + " has been verified.",
                    "/farmer/marketplace-orders/" + saved.getId());
        }
        return toPaymentProofResponse(saved);
    }

    @Transactional
    public MarketplacePaymentProofResponse rejectAdminPaymentProof(
            Long orderId,
            MarketplaceRejectPaymentProofRequest request) {
        MarketplaceOrder order = marketplaceOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        if (order.getPaymentMethod() != MarketplacePaymentMethod.BANK_TRANSFER) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }
        if (normalizeNullable(order.getPaymentProofStoragePath()) == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_REQUIRED);
        }
        if (order.getPaymentVerificationStatus() != MarketplacePaymentVerificationStatus.SUBMITTED) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }
        String reason = normalizeNullable(request.reason());
        if (reason == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        User admin = currentUserService.getCurrentUser();
        order.setPaymentVerificationStatus(MarketplacePaymentVerificationStatus.REJECTED);
        order.setPaymentVerifiedAt(LocalDateTime.now());
        order.setPaymentVerifiedByUserId(admin.getId());
        order.setPaymentVerificationNote(reason);

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        auditOrderOperation(saved, "PAYMENT_REJECTED", "Admin rejected payment proof: " + reason);

        if (saved.getBuyerUser() != null) {
            notifyUser(
                    saved.getBuyerUser().getId(),
                    "Payment rejected",
                    "Payment for order " + saved.getOrderCode() + " was rejected. Reason: " + reason,
                    "/marketplace/orders/" + saved.getId());
        }
        if (saved.getFarmerUser() != null) {
            notifyUser(
                    saved.getFarmerUser().getId(),
                    "Payment rejected",
                    "Payment for order " + saved.getOrderCode() + " was rejected.",
                    "/farmer/marketplace-orders/" + saved.getId());
        }
        return toPaymentProofResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceAddressResponse> listAddresses() {
        Long userId = currentUserService.getCurrentUserId();
        return marketplaceAddressRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(userId).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public MarketplaceAddressResponse createAddress(MarketplaceAddressUpsertRequest request) {
        User user = currentUserService.getCurrentUser();
        boolean shouldSetDefault = Boolean.TRUE.equals(request.isDefault()) || !marketplaceAddressRepository.existsByUser_IdAndDeletedAtIsNull(user.getId());
        if (shouldSetDefault) {
            marketplaceAddressRepository.clearDefaultByUserId(user.getId());
        }

        MarketplaceAddress address = MarketplaceAddress.builder()
                .user(user)
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .province(request.province().trim())
                .district(request.district().trim())
                .ward(request.ward().trim())
                .street(request.street().trim())
                .detail(normalizeNullable(request.detail()))
                .label(normalizeAddressLabel(request.label()))
                .isDefault(shouldSetDefault)
                .build();

        return toAddressResponse(marketplaceAddressRepository.save(address));
    }

    @Transactional
    public MarketplaceAddressResponse updateAddress(Long addressId, MarketplaceAddressUpsertRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceAddress address = marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND));

        if (Boolean.TRUE.equals(request.isDefault())) {
            marketplaceAddressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        }

        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setStreet(request.street().trim());
        address.setDetail(normalizeNullable(request.detail()));
        address.setLabel(normalizeAddressLabel(request.label()));

        return toAddressResponse(marketplaceAddressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceAddress address = marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        address.setIsDefault(false);
        address.setDeletedAt(LocalDateTime.now());
        marketplaceAddressRepository.save(address);

        if (wasDefault) {
            List<MarketplaceAddress> remaining = marketplaceAddressRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(userId);
            if (!remaining.isEmpty()) {
                MarketplaceAddress first = remaining.getFirst();
                first.setIsDefault(true);
                marketplaceAddressRepository.save(first);
            }
        }
    }

    @Transactional
    public MarketplaceAddressResponse setDefaultAddress(Long addressId) {
        Long userId = currentUserService.getCurrentUserId();
        MarketplaceAddress address = marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND));

        marketplaceAddressRepository.clearDefaultByUserId(userId);
        address.setIsDefault(true);

        return toAddressResponse(marketplaceAddressRepository.save(address));
    }




    @Transactional(readOnly = true)
    public MarketplaceFarmerDashboardResponse getFarmerDashboard() {
        Long farmerUserId = currentUserService.getCurrentUserId();

        long totalProducts = marketplaceProductRepository.countByFarmerUser_Id(farmerUserId);
        long pendingReviewProducts = marketplaceProductRepository.countByFarmerUser_IdAndStatus(
                farmerUserId,
                MarketplaceProductStatus.PENDING_REVIEW);
        long publishedProducts = marketplaceProductRepository.countByFarmerUser_IdAndStatusIn(
                farmerUserId,
                SELLABLE_PRODUCT_STATUSES);
        long lowStockProducts = productWarehouseLotRepository.countMarketplaceListingsByFarmerAndOnHandLessThanEqual(
                farmerUserId,
                SELLABLE_PRODUCT_STATUSES,
                LOW_STOCK_THRESHOLD);

        long pendingOrders = marketplaceOrderRepository.countByFarmerUser_IdAndStatus(
                farmerUserId,
                MarketplaceOrderStatus.PENDING_PAYMENT);
        long totalOrders = marketplaceOrderRepository.countByFarmerUser_Id(farmerUserId);
        long completedOrders = marketplaceOrderRepository.countByFarmerUser_IdAndStatus(
                farmerUserId,
                MarketplaceOrderStatus.COMPLETED);
        BigDecimal revenueValue = marketplaceOrderRepository.sumTotalAmountByFarmerUserIdAndStatus(
                farmerUserId,
                MarketplaceOrderStatus.COMPLETED);
        LocalDateTime lastOrderAt = marketplaceOrderRepository.findLastOrderAtByFarmerUserId(farmerUserId);

        boolean hasProducts = totalProducts > 0;
        boolean hasOrders = totalOrders > 0;
        boolean hasRevenueData = completedOrders > 0;
        BigDecimal totalRevenue = hasRevenueData ? Optional.ofNullable(revenueValue).orElse(BigDecimal.ZERO) : null;
        List<String> unavailableReasons = buildMarketplaceUnavailableReasons(hasProducts, hasOrders, hasRevenueData);

        List<MarketplaceOrderResponse> recentOrders = marketplaceOrderRepository
                .findRecentByFarmerUserId(farmerUserId, PageRequest.of(0, 5))
                .stream()
                .map(this::toOrderResponse)
                .toList();

        return new MarketplaceFarmerDashboardResponse(
                totalProducts,
                pendingReviewProducts,
                publishedProducts,
                lowStockProducts,
                pendingOrders,
                totalRevenue,
                hasProducts,
                hasOrders,
                hasRevenueData,
                lastOrderAt,
                unavailableReasons,
                recentOrders);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceProductSummaryResponse> listFarmerProducts(
            String q,
            MarketplaceProductStatus status,
            int page,
            int size) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<MarketplaceProduct> productPage = marketplaceProductRepository.searchFarmerProducts(
                farmerUserId,
                status,
                normalizeNullable(q),
                pageable);

        Map<Long, MarketplaceProductReviewRepository.ProductRatingProjection> ratings = aggregateProductRatings(
                productPage.getContent().stream().map(MarketplaceProduct::getId).toList());

        List<MarketplaceProductSummaryResponse> items = productPage.getContent().stream()
                .map(product -> toProductSummary(product, ratings.get(product.getId())))
                .toList();

        return PageResponse.of(productPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceFarmerProductFormOptionsResponse getFarmerProductFormOptions() {
        Long farmerUserId = currentUserService.getCurrentUserId();

        List<Farm> farms = farmRepository.findAllByUser_Id(farmerUserId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((Farm farm) -> normalizeNullable(farm.getName()), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Farm::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Season> seasons = seasonRepository.findAllByFarmUserId(farmerUserId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((Season season) -> normalizeNullable(season.getSeasonName()),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Season::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<ProductWarehouseLot> lots = productWarehouseLotRepository.findSellableByFarmUserId(farmerUserId);
        Map<Integer, MarketplaceProduct> linkedProductByLotId = lots.isEmpty()
                ? Collections.emptyMap()
                : marketplaceProductRepository.findAllByLot_IdIn(lots.stream().map(ProductWarehouseLot::getId).toList()).stream()
                        .filter(product -> product.getLot() != null && product.getLot().getId() != null)
                        .collect(Collectors.toMap(product -> product.getLot().getId(), product -> product));

        return new MarketplaceFarmerProductFormOptionsResponse(
                farms.stream().map(this::toFarmerProductFormFarmOption).toList(),
                seasons.stream().map(this::toFarmerProductFormSeasonOption).toList(),
                lots.stream()
                        .map(lot -> toFarmerProductFormLotOption(lot, linkedProductByLotId.get(lot.getId())))
                        .toList());
    }

    @Transactional(readOnly = true)
    public MarketplaceProductDetailResponse getFarmerProductDetail(Long productId) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        MarketplaceProduct product = getOwnedProductForFarmer(productId, farmerUserId);
        MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(product.getId()))
                .get(product.getId());
        return toProductDetail(product, rating);
    }

    @Transactional
    public MarketplaceProductDetailResponse createFarmerProduct(MarketplaceFarmerProductUpsertRequest request) {
        User farmer = currentUserService.getCurrentUser();
        ProductWarehouseLot lot = resolveSellableOwnedLot(request.lotId(), farmer.getId());
        validateFarmerListingStock(request.stockQuantity(), lot);

        List<String> imageUrls = normalizeImageUrls(request.imageUrls(), request.imageUrl());
        String primaryImage = imageUrls.isEmpty() ? normalizeNullable(request.imageUrl()) : imageUrls.get(0);

        MarketplaceProduct existingProduct = marketplaceProductRepository.findByLot_Id(lot.getId()).orElse(null);
        if (existingProduct != null) {
            if (!Objects.equals(existingProduct.getFarmerUser().getId(), farmer.getId())) {
                throw new AppException(ErrorCode.NOT_OWNER);
            }
            return saveFarmerProduct(existingProduct, request, farmer, lot, primaryImage, imageUrls);
        }

        MarketplaceProduct product = MarketplaceProduct.builder()
                .slug(generateUniqueSlug(request.name(), null))
                .name(request.name().trim())
                .category(normalizeNullable(request.category()))
                .shortDescription(normalizeNullable(request.shortDescription()))
                .description(normalizeNullable(request.description()))
                .price(request.price())
                .unit(lot.getUnit())
                .stockQuantity(request.stockQuantity())
                .imageUrl(primaryImage)
                .imageUrlsJson(toImageUrlsJson(imageUrls))
                .farmerUser(farmer)
                .farm(lot.getFarm())
                .season(lot.getSeason())
                .lot(lot)
                .traceable(Boolean.TRUE)
                .status(MarketplaceProductStatus.DRAFT)
                .publishedAt(null)
                .build();

        MarketplaceProduct saved = marketplaceProductRepository.save(product);
        return toProductDetail(saved, null);
    }

    @Transactional
    public MarketplaceProductDetailResponse updateFarmerProduct(
            Long productId,
            MarketplaceFarmerProductUpsertRequest request) {
        User farmer = currentUserService.getCurrentUser();
        MarketplaceProduct product = getOwnedProductForFarmer(productId, farmer.getId());
        ProductWarehouseLot lot = resolveOwnedLotForUpsert(request.lotId(), farmer.getId(), product);
        validateFarmerListingStock(request.stockQuantity(), lot);

        List<String> imageUrls = normalizeImageUrls(request.imageUrls(), request.imageUrl());
        String primaryImage = imageUrls.isEmpty() ? normalizeNullable(request.imageUrl()) : imageUrls.get(0);

        return saveFarmerProduct(product, request, farmer, lot, primaryImage, imageUrls);
    }

    @Transactional
    public MarketplaceProductDetailResponse updateFarmerProductStatus(
            Long productId,
            MarketplaceUpdateProductStatusRequest request) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        MarketplaceProduct product = getOwnedProductForFarmer(productId, farmerUserId);
        MarketplaceProductStatus targetStatus = request.status();

        validateFarmerProductStatusTransition(product.getStatus(), targetStatus);
        if (targetStatus == MarketplaceProductStatus.PENDING_REVIEW) {
            validateTraceabilityChain(product);
            ensureLotSellable(product.getLot());
            ensureListingHasStock(product);
        }
        product.setStatus(targetStatus);

        // Set publishedAt timestamp for ACTIVE and legacy PUBLISHED status
        if (targetStatus == MarketplaceProductStatus.ACTIVE || targetStatus == MarketplaceProductStatus.PUBLISHED) {
            if (product.getPublishedAt() == null) {
                product.setPublishedAt(LocalDateTime.now());
            }
        } else if (targetStatus == MarketplaceProductStatus.SOLD_OUT) {
            // Preserve publishedAt for sold out products (they were previously ACTIVE)
        } else {
            // Clear publishedAt for other non-active statuses
            product.setPublishedAt(null);
        }

        MarketplaceProduct saved = marketplaceProductRepository.save(product);
        MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(saved.getId()))
                .get(saved.getId());
        return toProductDetail(saved, rating);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceOrderResponse> listFarmerOrders(
            MarketplaceOrderStatus status,
            int page,
            int size) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MarketplaceOrder> orderPage = status == null
                ? marketplaceOrderRepository.findByFarmerUser_Id(farmerUserId, pageable)
                : marketplaceOrderRepository.findByFarmerUser_IdAndStatus(farmerUserId, status, pageable);

        List<MarketplaceOrderResponse> items = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return PageResponse.of(orderPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceOrderResponse getFarmerOrderDetail(Long orderId) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(orderId, farmerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Transactional
    public MarketplaceOrderResponse updateFarmerOrderStatus(
            Long orderId,
            MarketplaceUpdateOrderStatusRequest request) {
        Long farmerUserId = currentUserService.getCurrentUserId();
        MarketplaceOrder order = marketplaceOrderRepository.findByIdAndFarmerUserIdWithItems(orderId, farmerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        MarketplaceOrderStatus targetStatus = request.status();

        validateFarmerOrderStatusTransition(order.getStatus(), targetStatus);
        if (targetStatus == MarketplaceOrderStatus.CANCELLED) {
            restoreOrderStock(order, currentUserService.getCurrentUser(), "Farmer cancelled order");
        }
        if (targetStatus == MarketplaceOrderStatus.REJECTED) {
            restoreOrderStock(order, currentUserService.getCurrentUser(), "Farmer rejected order");
        }

        order.setStatus(targetStatus);
        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        auditOrderOperation(saved, "ORDER_STATUS_CHANGED", "Farmer changed order status to " + targetStatus.name());
        if (saved.getBuyerUser() != null) {
            notifyUser(
                    saved.getBuyerUser().getId(),
                    "Order status updated",
                    "Order " + saved.getOrderCode() + " is now " + targetStatus.name() + ".",
                    "/marketplace/orders/" + saved.getId());
        }
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceProductSummaryResponse> listAdminProducts(
            String q,
            MarketplaceProductStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<MarketplaceProduct> productPage = marketplaceProductRepository.searchAdminProducts(
                status,
                normalizeNullable(q),
                pageable);

        Map<Long, MarketplaceProductReviewRepository.ProductRatingProjection> ratings = aggregateProductRatings(
                productPage.getContent().stream().map(MarketplaceProduct::getId).toList());

        List<MarketplaceProductSummaryResponse> items = productPage.getContent().stream()
                .map(product -> toProductSummary(product, ratings.get(product.getId())))
                .toList();
        return PageResponse.of(productPage, items);
    }

    @Transactional
    public MarketplaceProductDetailResponse updateAdminProductStatus(
            Long productId,
            MarketplaceUpdateProductStatusRequest request) {
        MarketplaceProduct product = marketplaceProductRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));
        MarketplaceProductStatus targetStatus = request.status();

        validateAdminProductStatusTransition(product.getStatus(), targetStatus);
        if (targetStatus == MarketplaceProductStatus.PUBLISHED) {
            ProductWarehouseLot lot = product.getLot();
            if (lot == null) {
                throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
            }
            ProductWarehouseLot lockedLot = productWarehouseLotRepository.findById(lot.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND));
            ensureLotSellable(lockedLot);
            ensureListingHasStock(product);
        }
        if (targetStatus == MarketplaceProductStatus.ACTIVE) {
            ProductWarehouseLot lot = product.getLot();
            if (lot == null) {
                throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
            }
            ProductWarehouseLot lockedLot = productWarehouseLotRepository.findById(lot.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND));
            ensureLotSellable(lockedLot);
            ensureListingHasStock(product);
        }
        product.setStatus(targetStatus);

        // Set publishedAt timestamp for ACTIVE and legacy PUBLISHED status
        if (targetStatus == MarketplaceProductStatus.ACTIVE || targetStatus == MarketplaceProductStatus.PUBLISHED) {
            if (product.getPublishedAt() == null) {
                product.setPublishedAt(LocalDateTime.now());
            }
        } else if (targetStatus == MarketplaceProductStatus.SOLD_OUT) {
            // Preserve publishedAt for sold out products (they were previously ACTIVE)
        } else {
            // Clear publishedAt for other non-active statuses
            product.setPublishedAt(null);
        }

        MarketplaceProduct saved = marketplaceProductRepository.save(product);
        auditProductStatusChange(saved, "Admin changed product status to " + targetStatus.name());
        if (saved.getFarmerUser() != null) {
            notifyUser(
                    saved.getFarmerUser().getId(),
                    "Product moderation updated",
                    "Product " + saved.getName() + " is now " + targetStatus.name() + ".",
                    "/farmer/marketplace-products");
        }
        MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(saved.getId()))
                .get(saved.getId());
        return toProductDetail(saved, rating);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketplaceOrderResponse> listAdminOrders(
            MarketplaceOrderStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MarketplaceOrder> orderPage = status == null
                ? marketplaceOrderRepository.findAll(pageable)
                : marketplaceOrderRepository.findByStatus(status, pageable);
        List<MarketplaceOrderResponse> items = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return PageResponse.of(orderPage, items);
    }

    @Transactional(readOnly = true)
    public MarketplaceOrderResponse getAdminOrderDetail(Long orderId) {
        MarketplaceOrder order = marketplaceOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Transactional
    public MarketplaceOrderResponse updateAdminPaymentVerification(
            Long orderId,
            MarketplaceUpdatePaymentVerificationRequest request) {
        MarketplaceOrder order = marketplaceOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        if (order.getPaymentMethod() != MarketplacePaymentMethod.BANK_TRANSFER) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }
        if (normalizeNullable(order.getPaymentProofStoragePath()) == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_REQUIRED);
        }
        MarketplacePaymentVerificationStatus target = request.verificationStatus();
        if (target != MarketplacePaymentVerificationStatus.VERIFIED
                && target != MarketplacePaymentVerificationStatus.REJECTED) {
            throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_VERIFICATION_INVALID);
        }

        User admin = currentUserService.getCurrentUser();
        order.setPaymentVerificationStatus(target);
        order.setPaymentVerificationNote(normalizeNullable(request.verificationNote()));
        order.setPaymentVerifiedAt(LocalDateTime.now());
        order.setPaymentVerifiedByUserId(admin.getId());

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        String operation = target == MarketplacePaymentVerificationStatus.VERIFIED
                ? "PAYMENT_VERIFIED"
                : "PAYMENT_REJECTED";
        auditOrderOperation(saved, operation, "Admin updated payment verification to " + target.name());
        if (saved.getBuyerUser() != null) {
            notifyUser(
                    saved.getBuyerUser().getId(),
                    "Payment verification updated",
                    "Payment for order " + saved.getOrderCode() + " is now " + target.name() + ".",
                    "/marketplace/orders/" + saved.getId());
        }
        return toOrderResponse(saved);
    }

    @Transactional
    public MarketplaceOrderResponse updateAdminOrderStatus(
            Long orderId,
            MarketplaceUpdateOrderStatusRequest request) {
        MarketplaceOrder order = marketplaceOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        if (request.status() != MarketplaceOrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (order.getStatus() == MarketplaceOrderStatus.CANCELLED
                || order.getStatus() == MarketplaceOrderStatus.REJECTED) {
            return toOrderResponse(order);
        }
        if (order.getStatus() == MarketplaceOrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.MARKETPLACE_ORDER_CANCEL_NOT_ALLOWED);
        }

        restoreOrderStock(order, currentUserService.getCurrentUser(), "Admin cancelled order");
        order.setStatus(MarketplaceOrderStatus.CANCELLED);
        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        auditOrderOperation(saved, "ORDER_CANCELLED", "Admin cancelled order");

        if (saved.getBuyerUser() != null) {
            notifyUser(
                    saved.getBuyerUser().getId(),
                    "Order cancelled",
                    "Admin cancelled order " + saved.getOrderCode() + ".",
                    "/marketplace/orders/" + saved.getId());
        }
        if (saved.getFarmerUser() != null) {
            notifyUser(
                    saved.getFarmerUser().getId(),
                    "Order cancelled",
                    "Admin cancelled order " + saved.getOrderCode() + ".",
                    "/farmer/marketplace-orders/" + saved.getId());
        }
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceOrderAuditLogResponse> listOrderAuditLogs(Long orderId) {
        marketplaceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));
        return auditLogService.getEntityAuditTrail(AUDIT_ENTITY_ORDER, Math.toIntExact(orderId)).stream()
                .map(this::toOrderAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceAdminStatsResponse getAdminStats() {
        long totalProducts = marketplaceProductRepository.count();
        long pendingReviewProducts = marketplaceProductRepository.countByStatus(MarketplaceProductStatus.PENDING_REVIEW);
        long publishedProducts = marketplaceProductRepository.countByStatusIn(SELLABLE_PRODUCT_STATUSES);
        long hiddenProducts = marketplaceProductRepository.countByStatusIn(HIDDEN_PRODUCT_STATUSES);

        long totalOrders = marketplaceOrderRepository.count();
        long pendingOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.PENDING_PAYMENT);
        long confirmedOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.CONFIRMED);
        long preparingOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.PREPARING);
        long deliveringOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.SHIPPED);
        long completedOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.COMPLETED);
        long cancelledOrders = marketplaceOrderRepository.countByStatus(MarketplaceOrderStatus.CANCELLED);
        long pendingPaymentVerificationOrders = marketplaceOrderRepository.countByPaymentVerificationStatus(
                MarketplacePaymentVerificationStatus.SUBMITTED);
        long activeOrders = pendingOrders + confirmedOrders + preparingOrders + deliveringOrders;

        BigDecimal revenueValue = marketplaceOrderRepository.sumTotalAmountByStatus(MarketplaceOrderStatus.COMPLETED);
        LocalDateTime lastOrderAt = marketplaceOrderRepository.findLastOrderAt();

        boolean hasProducts = totalProducts > 0;
        boolean hasOrders = totalOrders > 0;
        boolean hasRevenueData = completedOrders > 0;
        BigDecimal totalRevenue = hasRevenueData ? Optional.ofNullable(revenueValue).orElse(BigDecimal.ZERO) : null;
        List<String> unavailableReasons = buildMarketplaceUnavailableReasons(hasProducts, hasOrders, hasRevenueData);

        return new MarketplaceAdminStatsResponse(
                totalProducts,
                pendingReviewProducts,
                publishedProducts,
                hiddenProducts,
                totalOrders,
                activeOrders,
                completedOrders,
                cancelledOrders,
                pendingPaymentVerificationOrders,
                totalRevenue,
                hasProducts,
                hasOrders,
                hasRevenueData,
                lastOrderAt,
                unavailableReasons);
    }

    private List<String> buildMarketplaceUnavailableReasons(boolean hasProducts, boolean hasOrders, boolean hasRevenueData) {
        List<String> reasons = new ArrayList<>();
        if (!hasProducts) {
            reasons.add("NO_PRODUCTS");
        }
        if (!hasOrders) {
            reasons.add("NO_ORDERS");
        }
        if (!hasRevenueData) {
            reasons.add(hasOrders ? "NO_COMPLETED_ORDERS" : "NO_REVENUE_DATA");
        }
        return reasons;
    }

    private MarketplaceCreateOrderResultResponse buildOrderResultForExistingGroup(Long orderGroupId, String groupCode) {
        List<MarketplaceOrder> existingOrders = marketplaceOrderRepository.findAllByOrderGroupIdWithItems(orderGroupId);
        List<MarketplaceOrderResponse> responses = existingOrders.stream()
                .map(this::toOrderResponse)
                .toList();
        return new MarketplaceCreateOrderResultResponse(groupCode, responses.size(), responses);
    }

    private ShippingSnapshot resolveShippingSnapshot(Long userId, MarketplaceCreateOrderRequest request) {
        MarketplaceAddress selectedAddress = null;
        if (request.addressId() != null) {
            selectedAddress = marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(request.addressId(), userId)
                    .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND));
        } else {
            selectedAddress = marketplaceAddressRepository.findFirstByUser_IdAndIsDefaultTrueAndDeletedAtIsNullOrderByIdDesc(userId)
                    .orElse(null);
        }

        String recipientName = normalizeNullable(request.shippingRecipientName());
        String phone = normalizeNullable(request.shippingPhone());
        String addressLine = normalizeNullable(request.shippingAddressLine());

        if (recipientName == null && selectedAddress != null) {
            recipientName = selectedAddress.getFullName();
        }
        if (phone == null && selectedAddress != null) {
            phone = selectedAddress.getPhone();
        }
        if (addressLine == null && selectedAddress != null) {
            addressLine = formatAddressLine(selectedAddress);
        }

        if (recipientName == null || phone == null || addressLine == null) {
            throw new AppException(ErrorCode.MARKETPLACE_ADDRESS_REQUIRED);
        }

        return new ShippingSnapshot(recipientName, phone, addressLine);
    }

    private String formatAddressLine(MarketplaceAddress address) {
        return String.format(
                "%s, %s, %s, %s",
                address.getStreet(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince());
    }

    private String resolveIdempotencyKey(String headerIdempotencyKey, String payloadIdempotencyKey) {
        String fromHeader = normalizeNullable(headerIdempotencyKey);
        if (fromHeader != null) {
            return fromHeader;
        }
        return normalizeNullable(payloadIdempotencyKey);
    }

    private String buildOrderFingerprint(MarketplaceCreateOrderRequest request, List<MarketplaceCartItem> cartItems) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("paymentMethod", request.paymentMethod());
            payload.put("addressId", request.addressId());
            payload.put("shippingRecipientName", normalizeNullable(request.shippingRecipientName()));
            payload.put("shippingPhone", normalizeNullable(request.shippingPhone()));
            payload.put("shippingAddressLine", normalizeNullable(request.shippingAddressLine()));
            payload.put("note", normalizeNullable(request.note()));

            List<Map<String, Object>> cartSnapshot = cartItems.stream()
                    .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                    .map(ci -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("productId", ci.getProduct().getId());
                        item.put("quantity", ci.getQuantity());
                        return item;
                    })
                    .toList();
            payload.put("cart", cartSnapshot);

            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private MarketplaceCart getOrCreateCartForUpdate(User user) {
        return marketplaceCartRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> marketplaceCartRepository.save(MarketplaceCart.builder().user(user).build()));
    }

    private MarketplaceCartResponse buildCartResponse(Long userId, MarketplaceCart cart) {
        List<MarketplaceCartItem> items = marketplaceCartItemRepository.findByCartIdWithProduct(cart.getId());
        BigDecimal itemCount = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;

        // Group items by seller (farmerUserId) and build item responses once
        Map<Long, List<MarketplaceCartItem>> itemsBySeller = new LinkedHashMap<>();
        Map<Long, MarketplaceCartItemResponse> itemResponseMap = new LinkedHashMap<>();

        for (MarketplaceCartItem item : items) {
            MarketplaceProduct product = item.getProduct();
            itemCount = itemCount.add(item.getQuantity());
            BigDecimal unitPrice = product.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(item.getQuantity()));

            // Build item response once and cache it
            MarketplaceCartItemResponse itemResponse = new MarketplaceCartItemResponse(
                    product.getId(),
                    product.getSlug(),
                    product.getName(),
                    product.getImageUrl(),
                    unitPrice,
                    item.getQuantity(),
                    currentAvailableQuantity(product),
                    product.getFarmerUser() == null ? null : product.getFarmerUser().getId(),
                    Boolean.TRUE.equals(product.getTraceable()));

            itemResponseMap.put(product.getId(), itemResponse);

            // Group by seller
            Long sellerId = product.getFarmerUser() == null ? null : product.getFarmerUser().getId();
            if (sellerId != null) {
                itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
            }
        }

        // Build flat items list (for backward compatibility - items appear in both flat list and seller groups)
        List<MarketplaceCartItemResponse> itemResponses = new ArrayList<>(itemResponseMap.values());

        // Build seller groups
        List<MarketplaceCartSellerGroupResponse> sellerGroups = new ArrayList<>();
        for (Map.Entry<Long, List<MarketplaceCartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<MarketplaceCartItem> sellerItems = entry.getValue();

            // Get farmer and farm info from first item (all items from same seller)
            MarketplaceCartItem firstItem = sellerItems.get(0);
            MarketplaceProduct firstProduct = firstItem.getProduct();
            User farmer = firstProduct.getFarmerUser();

            // Null safety: farmer should never be null here due to sellerId filter above,
            // but add defensive check to prevent NPE
            if (farmer == null) {
                continue; // Skip this group if farmer is unexpectedly null
            }

            Farm farm = firstProduct.getFarm();

            // Reuse pre-built item responses for this seller
            List<MarketplaceCartItemResponse> sellerItemResponses = new ArrayList<>();
            BigDecimal sellerSubtotal = BigDecimal.ZERO;

            for (MarketplaceCartItem item : sellerItems) {
                MarketplaceProduct product = item.getProduct();
                BigDecimal unitPrice = product.getPrice();
                sellerSubtotal = sellerSubtotal.add(unitPrice.multiply(item.getQuantity()));

                // Reuse the cached item response
                MarketplaceCartItemResponse cachedResponse = itemResponseMap.get(product.getId());
                sellerItemResponses.add(cachedResponse);
            }

            sellerGroups.add(new MarketplaceCartSellerGroupResponse(
                    farmer.getId(),
                    farmer.getFullName(),
                    farm == null ? null : farm.getId(),
                    farm == null ? null : farm.getName(),
                    sellerItemResponses,
                    sellerSubtotal));
        }

        return new MarketplaceCartResponse(
                userId,
                itemResponses,
                sellerGroups,
                itemCount,
                subtotal,
                CURRENCY_VND);
    }

    private MarketplaceCartResponse emptyCart(Long userId) {
        return new MarketplaceCartResponse(userId, Collections.emptyList(), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, CURRENCY_VND);
    }

    private List<MarketplaceProductStatus> buyerVisibleProductStatuses() {
        return SELLABLE_PRODUCT_STATUSES;
    }

    private MarketplaceProduct getCartProductOrThrow(
            Long productId,
            BigDecimal requestedQuantity,
            BigDecimal effectiveQuantity) {
        return marketplaceProductRepository.findByIdWithLotForCartValidation(productId)
                .orElseThrow(() -> {
                    logCartValidationFailure(
                            ErrorCode.PRODUCT_NOT_FOUND,
                            productId,
                            null,
                            null,
                            null,
                            null,
                            requestedQuantity,
                            effectiveQuantity);
                    return new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    private void validateCartProductAvailability(
            MarketplaceProduct product,
            BigDecimal requestedQuantity,
            BigDecimal effectiveQuantity) {
        ProductWarehouseLot lot = product.getLot();
        Long productId = product.getId();

        if (!buyerVisibleProductStatuses().contains(product.getStatus())) {
            logCartValidationFailure(
                    ErrorCode.PRODUCT_NOT_AVAILABLE,
                    productId,
                    product.getStatus(),
                    product.getStockQuantity(),
                    lot == null ? null : lot.getStatus(),
                    lot == null ? null : lot.getOnHandQuantity(),
                    requestedQuantity,
                    effectiveQuantity);
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        if (product.getStockQuantity() == null || product.getStockQuantity().compareTo(ZERO_QUANTITY) <= 0) {
            logCartValidationFailure(
                    ErrorCode.PRODUCT_OUT_OF_STOCK,
                    productId,
                    product.getStatus(),
                    product.getStockQuantity(),
                    lot == null ? null : lot.getStatus(),
                    lot == null ? null : lot.getOnHandQuantity(),
                    requestedQuantity,
                    effectiveQuantity);
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        if (lot == null
                || lot.getStatus() != ProductWarehouseLotStatus.IN_STOCK
                || lot.getOnHandQuantity() == null
                || lot.getOnHandQuantity().compareTo(ZERO_QUANTITY) <= 0) {
            logCartValidationFailure(
                    ErrorCode.PRODUCT_LOT_UNAVAILABLE,
                    productId,
                    product.getStatus(),
                    product.getStockQuantity(),
                    lot == null ? null : lot.getStatus(),
                    lot == null ? null : lot.getOnHandQuantity(),
                    requestedQuantity,
                    effectiveQuantity);
            throw new AppException(ErrorCode.PRODUCT_LOT_UNAVAILABLE);
        }

        BigDecimal availableQuantity = product.getStockQuantity().min(lot.getOnHandQuantity());
        if (availableQuantity.compareTo(effectiveQuantity) < 0) {
            logCartValidationFailure(
                    ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK,
                    productId,
                    product.getStatus(),
                    product.getStockQuantity(),
                    lot.getStatus(),
                    lot.getOnHandQuantity(),
                    requestedQuantity,
                    effectiveQuantity);
            throw new AppException(ErrorCode.MARKETPLACE_INSUFFICIENT_STOCK);
        }
    }

    private void logCartValidationFailure(
            ErrorCode errorCode,
            Long productId,
            MarketplaceProductStatus productStatus,
            BigDecimal stockQuantity,
            ProductWarehouseLotStatus lotStatus,
            BigDecimal lotOnHandQuantity,
            BigDecimal requestedQuantity,
            BigDecimal effectiveQuantity) {
        log.warn(
                "Marketplace cart validation failed: errorCode={}, productId={}, productStatus={}, stockQuantity={}, lotStatus={}, lotOnHandQuantity={}, requestedQuantity={}, effectiveQuantity={}",
                errorCode.getCode(),
                productId,
                productStatus,
                stockQuantity,
                lotStatus,
                lotOnHandQuantity,
                requestedQuantity,
                effectiveQuantity);
    }

    private void ensureStockAvailable(MarketplaceProduct product, BigDecimal requestedQuantity) {
        validatePositiveQuantity(requestedQuantity);
        if (currentAvailableQuantity(product).compareTo(requestedQuantity) < 0) {
            throw new AppException(ErrorCode.MARKETPLACE_STOCK_CONFLICT);
        }
    }

    private void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(ZERO_QUANTITY) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateTraceabilityChain(MarketplaceProduct product) {
        if (!Boolean.TRUE.equals(product.getTraceable())) {
            return;
        }
        if (product.getFarm() == null || product.getSeason() == null || product.getLot() == null) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }

        Integer farmId = product.getFarm().getId();
        Integer seasonFarmId = product.getSeason().getPlot() == null || product.getSeason().getPlot().getFarm() == null
                ? null
                : product.getSeason().getPlot().getFarm().getId();
        Integer lotFarmId = product.getLot().getFarm() == null ? null : product.getLot().getFarm().getId();
        Integer lotSeasonId = product.getLot().getSeason() == null ? null : product.getLot().getSeason().getId();

        if (!Objects.equals(farmId, seasonFarmId)
                || !Objects.equals(farmId, lotFarmId)
                || (lotSeasonId != null && !Objects.equals(product.getSeason().getId(), lotSeasonId))) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }
    }

    private Sort resolveProductSort(String sort) {
        if ("price_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }
        if ("price_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private MarketplaceProductSummaryResponse toProductSummary(
            MarketplaceProduct product,
            MarketplaceProductReviewRepository.ProductRatingProjection ratingProjection) {
        User farmer = product.getFarmerUser();
        Farm farm = product.getFarm();
        var season = product.getSeason();
        var lot = product.getLot();

        double averageRating = ratingProjection == null ? 0D : Optional.ofNullable(ratingProjection.getAverageRating()).orElse(0D);
        long ratingCount = ratingProjection == null ? 0L : Optional.ofNullable(ratingProjection.getRatingCount()).orElse(0L);

        return new MarketplaceProductSummaryResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getCategory(),
                product.getShortDescription(),
                product.getPrice(),
                resolveListingUnit(product),
                currentListingQuantity(product),
                currentAvailableQuantity(product),
                product.getImageUrl(),
                farmer == null ? null : farmer.getId(),
                farmer == null ? null : defaultDisplayName(farmer),
                farm == null ? null : farm.getId(),
                farm == null ? null : farm.getName(),
                season == null ? null : season.getId(),
                season == null ? null : season.getSeasonName(),
                lot == null ? null : lot.getId(),
                resolveFarmRegion(farm),
                Boolean.TRUE.equals(product.getTraceable()),
                averageRating,
                ratingCount,
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private MarketplaceProductDetailResponse toProductDetail(
            MarketplaceProduct product,
            MarketplaceProductReviewRepository.ProductRatingProjection ratingProjection) {
        MarketplaceProductSummaryResponse summary = toProductSummary(product, ratingProjection);
        return new MarketplaceProductDetailResponse(
                summary.id(),
                summary.slug(),
                summary.name(),
                summary.category(),
                summary.shortDescription(),
                product.getDescription(),
                summary.price(),
                summary.unit(),
                summary.stockQuantity(),
                summary.availableQuantity(),
                summary.imageUrl(),
                resolveImageUrls(product),
                summary.farmerUserId(),
                summary.farmerDisplayName(),
                summary.farmId(),
                summary.farmName(),
                summary.seasonId(),
                summary.seasonName(),
                summary.lotId(),
                summary.region(),
                summary.traceable(),
                product.getLot() == null ? null : product.getLot().getLotCode(),
                summary.ratingAverage(),
                summary.ratingCount(),
                summary.status(),
                summary.createdAt(),
                summary.updatedAt());
    }

    private List<String> resolveImageUrls(MarketplaceProduct product) {
        String imageUrlsJson = normalizeNullable(product.getImageUrlsJson());
        if (imageUrlsJson == null) {
            return product.getImageUrl() == null ? Collections.emptyList() : List.of(product.getImageUrl());
        }
        try {
            List<String> parsed = objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {
            });
            return parsed.stream()
                    .map(this::normalizeNullable)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            if (product.getImageUrl() == null) {
                return Collections.emptyList();
            }
            return List.of(product.getImageUrl());
        }
    }

    private MarketplaceFarmSummaryResponse toFarmSummary(Farm farm, long productCount) {
        String coverImage = marketplaceProductRepository
                .findSellableByFarmIdAndStatusInOrderByPublishedAtDescIdDesc(
                        farm.getId(),
                        buyerVisibleProductStatuses(),
                        PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(MarketplaceProduct::getImageUrl)
                .orElse(null);

        return new MarketplaceFarmSummaryResponse(
                farm.getId(),
                farm.getName(),
                resolveFarmRegion(farm),
                resolveFarmAddress(farm),
                coverImage,
                productCount);
    }

    private MarketplaceFarmerProductFormFarmOptionResponse toFarmerProductFormFarmOption(Farm farm) {
        return new MarketplaceFarmerProductFormFarmOptionResponse(
                farm.getId(),
                farm.getName());
    }

    private MarketplaceFarmerProductFormSeasonOptionResponse toFarmerProductFormSeasonOption(Season season) {
        Integer farmId = season.getPlot() == null || season.getPlot().getFarm() == null
                ? null
                : season.getPlot().getFarm().getId();
        return new MarketplaceFarmerProductFormSeasonOptionResponse(
                season.getId(),
                season.getSeasonName(),
                farmId);
    }

    private MarketplaceFarmerProductFormLotOptionResponse toFarmerProductFormLotOption(
            ProductWarehouseLot lot,
            MarketplaceProduct linkedProduct) {
        Farm farm = lot.getFarm();
        Season season = lot.getSeason();
        return new MarketplaceFarmerProductFormLotOptionResponse(
                lot.getId(),
                lot.getLotCode(),
                farm == null ? null : farm.getId(),
                farm == null ? null : farm.getName(),
                season == null ? null : season.getId(),
                season == null ? null : season.getSeasonName(),
                lot.getOnHandQuantity() == null ? ZERO_QUANTITY : lot.getOnHandQuantity().max(ZERO_QUANTITY),
                lot.getHarvestedAt(),
                lot.getUnit(),
                lot.getProductName(),
                lot.getProductVariant(),
                linkedProduct == null ? null : linkedProduct.getId(),
                linkedProduct == null ? null : linkedProduct.getStatus());
    }

    private Map<Integer, Long> aggregateFarmProductCounts(Collection<Integer> farmIds) {
        if (farmIds == null || farmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> result = new HashMap<>();
        for (MarketplaceProductRepository.FarmProductCountProjection projection : marketplaceProductRepository
                .countPublishedByFarmIds(farmIds, buyerVisibleProductStatuses())) {
            result.put(projection.getFarmId(), projection.getProductCount());
        }
        return result;
    }

    private Map<Long, MarketplaceProductReviewRepository.ProductRatingProjection> aggregateProductRatings(
            Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return marketplaceProductReviewRepository.aggregateRatingsByProductIds(productIds).stream()
                .collect(Collectors.toMap(MarketplaceProductReviewRepository.ProductRatingProjection::getProductId, p -> p));
    }

    private MarketplacePaymentVerificationStatus resolveInitialPaymentVerificationStatus(MarketplacePaymentMethod paymentMethod) {
        return paymentMethod == MarketplacePaymentMethod.BANK_TRANSFER
                ? MarketplacePaymentVerificationStatus.AWAITING_PROOF
                : MarketplacePaymentVerificationStatus.NOT_REQUIRED;
    }

    private void restoreOrderStock(MarketplaceOrder order, User actor, String reason) {
        List<Long> productIds = order.getItems().stream()
                .map(MarketplaceOrderItem::getProduct)
                .filter(Objects::nonNull)
                .map(MarketplaceProduct::getId)
                .distinct()
                .toList();
        List<MarketplaceProduct> lockedProducts = marketplaceProductRepository.findAllByIdInForUpdate(productIds);
        Map<Long, MarketplaceProduct> productById = lockedProducts.stream()
                .collect(Collectors.toMap(MarketplaceProduct::getId, product -> product));

        List<Integer> lotIds = order.getItems().stream()
                .map(MarketplaceOrderItem::getLot)
                .filter(Objects::nonNull)
                .map(ProductWarehouseLot::getId)
                .distinct()
                .toList();
        List<ProductWarehouseLot> lockedLots = productWarehouseLotRepository.findAllByIdInForUpdate(lotIds);
        Map<Integer, ProductWarehouseLot> lotById = lockedLots.stream()
                .collect(Collectors.toMap(ProductWarehouseLot::getId, lot -> lot));

        for (MarketplaceOrderItem orderItem : order.getItems()) {
            MarketplaceProduct orderProduct = orderItem.getProduct();
            Long productId = orderProduct == null ? null : orderProduct.getId();
            if (productId != null) {
                MarketplaceProduct product = productById.get(productId);
                if (product == null) {
                    log.warn("restoreOrderStock: product id={} no longer exists in DB, skipping stock restore for this product", productId);
                } else {
                    product.setStockQuantity(currentListingQuantity(product).add(orderItem.getQuantity()));
                }
            }

            ProductWarehouseLot orderLot = orderItem.getLot();
            Integer lotId = orderLot == null ? null : orderLot.getId();
            if (lotId == null) {
                continue;
            }
            ProductWarehouseLot lot = lotById.get(lotId);
            if (lot == null) {
                throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND);
            }
            lot.setOnHandQuantity(lot.getOnHandQuantity().add(orderItem.getQuantity()));
            productWarehouseTransactionRepository.save(buildMarketplaceLotTransaction(
                    lot,
                    ProductWarehouseTransactionType.MARKETPLACE_ORDER_RELEASED,
                    orderItem.getQuantity(),
                    "ORDER",
                    order.getOrderCode(),
                    reason,
                    actor));
        }

        marketplaceProductRepository.saveAll(lockedProducts);
        productWarehouseLotRepository.saveAll(lockedLots);
    }

    private void storePaymentProof(MarketplaceOrder order, MultipartFile file) {
        String root = appProperties.getMarketplace().getStorage().getPaymentProofRoot();
        String extension = extractExtension(file.getOriginalFilename());
        Path rootPath = Path.of(root).toAbsolutePath().normalize();
        Path orderDirectory = rootPath.resolve("order-" + order.getId());
        String storedFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;
        Path target = orderDirectory.resolve(storedFileName).normalize();

        try {
            if (!target.startsWith(rootPath)) {
                throw new AppException(ErrorCode.MARKETPLACE_PAYMENT_PROOF_INVALID);
            }
            Files.createDirectories(orderDirectory);
            if (normalizeNullable(order.getPaymentProofStoragePath()) != null) {
                Path existing = Path.of(order.getPaymentProofStoragePath()).toAbsolutePath().normalize();
                if (existing.startsWith(rootPath) && Files.exists(existing)) {
                    Files.delete(existing);
                }
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        order.setPaymentProofFileName(normalizeNullable(file.getOriginalFilename()));
        order.setPaymentProofContentType(normalizeNullable(file.getContentType()));
        order.setPaymentProofStoragePath(target.toString());
    }

    private String extractExtension(String fileName) {
        String normalized = normalizeNullable(fileName);
        if (normalized == null) {
            return "";
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(lastDot);
    }

    private void notifyUser(Long userId, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        notificationService.createNotification(userId, title, message, link);
    }

    private void auditOrderOperation(MarketplaceOrder order, String operation, String reason) {
        if (order == null || order.getId() == null) {
            return;
        }
        String performedBy = resolveCurrentUsername();
        auditLogService.logEntityOperation(
                AUDIT_ENTITY_ORDER,
                Math.toIntExact(order.getId()),
                operation,
                performedBy,
                order,
                reason,
                null);
    }

    private void auditProductStatusChange(MarketplaceProduct product, String reason) {
        if (product == null || product.getId() == null) {
            return;
        }
        auditLogService.logEntityOperation(
                "MARKETPLACE_PRODUCT",
                Math.toIntExact(product.getId()),
                "PRODUCT_STATUS_CHANGED",
                resolveCurrentUsername(),
                product,
                reason,
                null);
    }

    private String resolveCurrentUsername() {
        try {
            User currentUser = currentUserService.getCurrentUser();
            return defaultDisplayName(currentUser);
        } catch (Exception ex) {
            return "system";
        }
    }

    private MarketplaceOrderStatus normalizeBuyerOrderStatus(String status) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("PENDING".equals(upper)) {
            return MarketplaceOrderStatus.PENDING_PAYMENT;
        }
        if ("DELIVERING".equals(upper)) {
            return MarketplaceOrderStatus.SHIPPED;
        }
        try {
            return MarketplaceOrderStatus.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private Map<Long, Long> loadReviewIdsByProductForOrder(Long orderId, Long buyerUserId) {
        return marketplaceProductReviewRepository.findByOrder_IdAndBuyerUser_Id(orderId, buyerUserId).stream()
                .filter(review -> review.getProduct() != null && review.getProduct().getId() != null)
                .collect(Collectors.toMap(
                        review -> review.getProduct().getId(),
                        MarketplaceProductReview::getId,
                        (left, right) -> left));
    }

    private Map<Long, Map<Long, Long>> loadReviewIdsByOrderAndProduct(List<Long> orderIds, Long buyerUserId) {
        if (orderIds == null || orderIds.isEmpty() || buyerUserId == null) {
            return Collections.emptyMap();
        }
        return marketplaceProductReviewRepository.findByOrder_IdInAndBuyerUser_Id(orderIds, buyerUserId).stream()
                .filter(review -> review.getOrder() != null
                        && review.getOrder().getId() != null
                        && review.getProduct() != null
                        && review.getProduct().getId() != null)
                .collect(Collectors.groupingBy(
                        review -> review.getOrder().getId(),
                        Collectors.toMap(
                                review -> review.getProduct().getId(),
                                MarketplaceProductReview::getId,
                                (left, right) -> left)));
    }

    private MarketplaceOrderResponse toOrderResponse(MarketplaceOrder order) {
        Long currentUserId = null;
        try {
            currentUserId = currentUserService.getCurrentUserId();
        } catch (Exception ex) {
            currentUserId = null;
        }
        Long buyerUserId = order.getBuyerUser() == null ? null : order.getBuyerUser().getId();
        Map<Long, Long> reviewMap = currentUserId != null && Objects.equals(currentUserId, buyerUserId)
                ? loadReviewIdsByProductForOrder(order.getId(), currentUserId)
                : Collections.emptyMap();
        return toOrderResponse(order, currentUserId, reviewMap);
    }

    private MarketplaceOrderResponse toOrderResponse(
            MarketplaceOrder order,
            Long viewerUserId,
            Map<Long, Long> reviewMap) {
        Long buyerUserId = order.getBuyerUser() == null ? null : order.getBuyerUser().getId();
        List<MarketplaceOrderItemResponse> itemResponses = order.getItems().stream()
                .sorted(Comparator.comparing(MarketplaceOrderItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> new MarketplaceOrderItemResponse(
                        item.getId(),
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductNameSnapshot(),
                        item.getProductSlugSnapshot(),
                        item.getImageUrlSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getLineTotal(),
                        Boolean.TRUE.equals(item.getTraceableSnapshot()),
                        viewerUserId != null
                                && Objects.equals(viewerUserId, buyerUserId)
                                && (order.getStatus() == MarketplaceOrderStatus.COMPLETED)
                                && !reviewMap.containsKey(item.getProduct() == null ? null : item.getProduct().getId()),
                        reviewMap.get(item.getProduct() == null ? null : item.getProduct().getId())))
                .toList();

        return new MarketplaceOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getOrderGroup() == null ? null : order.getOrderGroup().getGroupCode(),
                order.getBuyerUser() == null ? null : order.getBuyerUser().getId(),
                order.getFarmerUser() == null ? null : order.getFarmerUser().getId(),
                order.getStatus(),
                toOrderPaymentResponse(order),
                order.getShippingRecipientName(),
                order.getShippingPhone(),
                order.getShippingAddressLine(),
                order.getNote(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                viewerUserId != null
                        && Objects.equals(viewerUserId, buyerUserId)
                        && (order.getStatus() == MarketplaceOrderStatus.PENDING_PAYMENT
                                || order.getStatus() == MarketplaceOrderStatus.PAYMENT_SUBMITTED
                                || order.getStatus() == MarketplaceOrderStatus.CONFIRMED),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses);
    }

    private MarketplaceOrderPaymentResponse toOrderPaymentResponse(MarketplaceOrder order) {
        return new MarketplaceOrderPaymentResponse(
                order.getPaymentMethod(),
                order.getPaymentVerificationStatus(),
                order.getPaymentProofFileName(),
                order.getPaymentProofContentType(),
                order.getPaymentProofStoragePath(),
                order.getPaymentProofUploadedAt(),
                order.getPaymentVerifiedAt(),
                order.getPaymentVerifiedByUserId(),
                order.getPaymentVerificationNote());
    }

    private MarketplacePaymentProofResponse toPaymentProofResponse(MarketplaceOrder order) {
        return new MarketplacePaymentProofResponse(
                order.getId(),
                order.getOrderCode(),
                order.getBuyerUser() == null ? null : order.getBuyerUser().getId(),
                order.getPaymentProofFileName(),
                order.getPaymentProofContentType(),
                order.getPaymentProofStoragePath(),
                order.getPaymentProofUploadedAt(),
                order.getPaymentVerificationStatus(),
                order.getPaymentVerificationNote(),
                order.getPaymentVerifiedAt(),
                order.getPaymentVerifiedByUserId());
    }

    private MarketplaceAddressResponse toAddressResponse(MarketplaceAddress address) {
        return new MarketplaceAddressResponse(
                address.getId(),
                address.getUser() == null ? null : address.getUser().getId(),
                address.getFullName(),
                address.getPhone(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getStreet(),
                address.getDetail(),
                address.getLabel(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }

    private MarketplaceReviewResponse toReviewResponse(MarketplaceProductReview review) {
        User buyer = review.getBuyerUser();
        return new MarketplaceReviewResponse(
                review.getId(),
                review.getProduct() == null ? null : review.getProduct().getId(),
                review.getOrder() == null ? null : review.getOrder().getId(),
                review.getOrderItem() == null ? null : review.getOrderItem().getId(),
                buyer == null ? null : buyer.getId(),
                buyer == null ? null : defaultDisplayName(buyer),
                review.getRating(),
                review.getComment(),
                Boolean.TRUE.equals(review.getHidden()),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private void recalculateProductRating(Long productId) {
        if (productId == null) return;
        MarketplaceProduct product = marketplaceProductRepository.findById(productId).orElse(null);
        if (product == null) return;

        MarketplaceProductReviewRepository.SingleProductRatingProjection projection =
                marketplaceProductReviewRepository.aggregateRatingByProductId(productId);
        double avg = projection == null || projection.getAverageRating() == null ? 0.0 : projection.getAverageRating();
        long count = projection == null || projection.getRatingCount() == null ? 0L : projection.getRatingCount();

        product.setAverageRating(Math.round(avg * 100.0) / 100.0);
        product.setRatingCount((int) count);
        marketplaceProductRepository.save(product);
    }

    private void recalculateFarmRating(Integer farmId) {
        if (farmId == null) return;
        Farm farm = farmRepository.findById(farmId).orElse(null);
        if (farm == null) return;

        MarketplaceProductReviewRepository.SingleProductRatingProjection projection =
                marketplaceProductReviewRepository.aggregateRatingByFarmId(farmId);
        double avg = projection == null || projection.getAverageRating() == null ? 0.0 : projection.getAverageRating();
        long count = projection == null || projection.getRatingCount() == null ? 0L : projection.getRatingCount();

        farm.setAverageRating(Math.round(avg * 100.0) / 100.0);
        farm.setRatingCount((int) count);
        farmRepository.save(farm);
    }

    private MarketplaceOrderAuditLogResponse toOrderAuditLogResponse(AuditLog auditLog) {
        return new MarketplaceOrderAuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOperation(),
                auditLog.getPerformedBy(),
                auditLog.getPerformedAt(),
                auditLog.getSnapshotDataJson(),
                auditLog.getReason(),
                auditLog.getIpAddress());
    }

    private String defaultDisplayName(User user) {
        String fullName = normalizeNullable(user.getFullName());
        if (fullName != null) {
            return fullName;
        }
        String username = normalizeNullable(user.getUsername());
        if (username != null) {
            return username;
        }
        return user.getEmail();
    }

    private String resolveFarmRegion(Farm farm) {
        if (farm == null || farm.getProvince() == null) {
            return null;
        }
        return farm.getProvince().getName();
    }

    private String resolveFarmAddress(Farm farm) {
        if (farm == null) {
            return null;
        }
        String wardName = farm.getWard() == null ? null : normalizeNullable(farm.getWard().getName());
        String provinceName = farm.getProvince() == null ? null : normalizeNullable(farm.getProvince().getName());
        if (wardName == null && provinceName == null) {
            return null;
        }
        if (wardName == null) {
            return provinceName;
        }
        if (provinceName == null) {
            return wardName;
        }
        return wardName + ", " + provinceName;
    }

    private MarketplaceProduct getOwnedProductForFarmer(Long productId, Long farmerUserId) {
        MarketplaceProduct product = marketplaceProductRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));
        Long ownerId = product.getFarmerUser() == null ? null : product.getFarmerUser().getId();
        if (!Objects.equals(ownerId, farmerUserId)) {
            throw new AppException(ErrorCode.NOT_OWNER);
        }
        return product;
    }

    private Farm resolveOwnedFarm(Integer farmId, Long farmerUserId) {
        return farmRepository.findByIdAndUserId(farmId, farmerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_OWNER));
    }

    private Season resolveOwnedSeason(Integer seasonId, Long farmerUserId) {
        if (seasonId == null) {
            return null;
        }
        return seasonRepository.findByIdAndFarmUserId(seasonId, farmerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_OWNER));
    }

    private ProductWarehouseLot resolveOwnedLot(Integer lotId, Long farmerUserId) {
        if (lotId == null) {
            return null;
        }
        return productWarehouseLotRepository.findByIdAndFarmUserId(lotId, farmerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_OWNER));
    }

    private ProductWarehouseLot resolveSellableOwnedLot(Integer lotId, Long farmerUserId) {
        return productWarehouseLotRepository
                .findByIdAndFarmUserIdAndStatusAndOnHandQuantityGreaterThan(
                        lotId,
                        farmerUserId,
                        ProductWarehouseLotStatus.IN_STOCK,
                        ZERO_QUANTITY)
                .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_STOCK_CONFLICT));
    }

    private ProductWarehouseLot resolveOwnedLotForUpsert(Integer lotId, Long farmerUserId, MarketplaceProduct currentProduct) {
        ProductWarehouseLot lot = resolveOwnedLot(lotId, farmerUserId);
        ProductWarehouseLot currentLot = currentProduct == null ? null : currentProduct.getLot();
        boolean sameLot = currentLot != null && Objects.equals(currentLot.getId(), lot.getId());
        if (!sameLot) {
            ensureLotSellable(lot);
        }
        if (currentProduct != null && marketplaceProductRepository.existsByLot_IdAndIdNot(lot.getId(), currentProduct.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        return lot;
    }

    private void validateTraceabilityReferencesForUpsert(
            boolean traceable,
            Farm farm,
            Season season,
            ProductWarehouseLot lot) {
        if (!traceable) {
            return;
        }

        if (farm == null || season == null || lot == null) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }

        Integer seasonFarmId = season.getPlot() == null || season.getPlot().getFarm() == null
                ? null
                : season.getPlot().getFarm().getId();
        Integer lotFarmId = lot.getFarm() == null ? null : lot.getFarm().getId();
        Integer lotSeasonId = lot.getSeason() == null ? null : lot.getSeason().getId();

        if (!Objects.equals(farm.getId(), seasonFarmId)
                || !Objects.equals(farm.getId(), lotFarmId)
                || !Objects.equals(season.getId(), lotSeasonId)) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }
    }

    private MarketplaceProductDetailResponse saveFarmerProduct(
            MarketplaceProduct product,
            MarketplaceFarmerProductUpsertRequest request,
            User farmer,
            ProductWarehouseLot lot,
            String primaryImage,
            List<String> imageUrls) {
        if (product.getId() != null && marketplaceProductRepository.existsByLot_IdAndIdNot(lot.getId(), product.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        validateTraceabilityReferencesForUpsert(true, lot.getFarm(), lot.getSeason(), lot);
        product.setSlug(generateUniqueSlug(request.name(), product.getId()));
        product.setName(request.name().trim());
        product.setCategory(normalizeNullable(request.category()));
        product.setShortDescription(normalizeNullable(request.shortDescription()));
        product.setDescription(normalizeNullable(request.description()));
        product.setPrice(request.price());
        product.setUnit(lot.getUnit());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(primaryImage);
        product.setImageUrlsJson(toImageUrlsJson(imageUrls));
        product.setFarmerUser(farmer);
        product.setFarm(lot.getFarm());
        product.setSeason(lot.getSeason());
        product.setLot(lot);
        product.setTraceable(Boolean.TRUE);
        product.setStatus(MarketplaceProductStatus.DRAFT);
        product.setPublishedAt(null);

        MarketplaceProduct saved = marketplaceProductRepository.save(product);
        MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(saved.getId()))
                .get(saved.getId());
        return toProductDetail(saved, rating);
    }

    private void ensureLotSellable(ProductWarehouseLot lot) {
        if (lot == null
                || lot.getStatus() != ProductWarehouseLotStatus.IN_STOCK
                || lot.getOnHandQuantity() == null
                || lot.getOnHandQuantity().compareTo(ZERO_QUANTITY) <= 0) {
            throw new AppException(ErrorCode.MARKETPLACE_STOCK_CONFLICT);
        }
    }

    private void ensureListingHasStock(MarketplaceProduct product) {
        if (currentListingQuantity(product).compareTo(ZERO_QUANTITY) <= 0) {
            throw new AppException(ErrorCode.MARKETPLACE_STOCK_CONFLICT);
        }
    }

    private void validateFarmerListingStock(BigDecimal stockQuantity, ProductWarehouseLot lot) {
        validatePositiveQuantity(stockQuantity);
        ensureLotSellable(lot);
        if (lot.getOnHandQuantity() == null || stockQuantity.compareTo(lot.getOnHandQuantity()) > 0) {
            throw new AppException(ErrorCode.MARKETPLACE_STOCK_CONFLICT);
        }
    }

    private ProductWarehouseLot resolveLockedLot(
            MarketplaceProduct product,
            Map<Integer, ProductWarehouseLot> lockedLotById) {
        ProductWarehouseLot productLot = product.getLot();
        Integer lotId = productLot == null ? null : productLot.getId();
        if (lotId == null) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }
        ProductWarehouseLot lockedLot = lockedLotById.get(lotId);
        if (lockedLot == null) {
            throw new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND);
        }
        return lockedLot;
    }

    private ProductWarehouseTransaction buildMarketplaceLotTransaction(
            ProductWarehouseLot lot,
            ProductWarehouseTransactionType transactionType,
            BigDecimal quantity,
            String referenceType,
            String referenceId,
            String note,
            User actor) {
        return ProductWarehouseTransaction.builder()
                .lot(lot)
                .transactionType(transactionType)
                .quantity(quantity)
                .unit(lot.getUnit())
                .resultingOnHand(lot.getOnHandQuantity())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note)
                .createdBy(actor)
                .build();
    }

    private BigDecimal currentAvailableQuantity(MarketplaceProduct product) {
        if (product == null || product.getLot() == null || product.getLot().getOnHandQuantity() == null) {
            return ZERO_QUANTITY;
        }
        return currentListingQuantity(product).min(product.getLot().getOnHandQuantity().max(ZERO_QUANTITY));
    }

    private BigDecimal currentListingQuantity(MarketplaceProduct product) {
        if (product == null || product.getStockQuantity() == null) {
            return ZERO_QUANTITY;
        }
        return product.getStockQuantity().max(ZERO_QUANTITY);
    }

    private String resolveListingUnit(MarketplaceProduct product) {
        if (product != null && product.getLot() != null && normalizeNullable(product.getLot().getUnit()) != null) {
            return product.getLot().getUnit();
        }
        return product == null ? null : product.getUnit();
    }

    private List<String> normalizeImageUrls(List<String> imageUrls, String fallbackImageUrl) {
        List<String> normalized = new ArrayList<>();
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                String clean = normalizeNullable(imageUrl);
                if (clean != null && !normalized.contains(clean)) {
                    normalized.add(clean);
                }
            }
        }
        String fallback = normalizeNullable(fallbackImageUrl);
        if (normalized.isEmpty() && fallback != null) {
            normalized.add(fallback);
        }
        return normalized;
    }

    private String toImageUrlsJson(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (Exception ex) {
            return null;
        }
    }

    private String generateUniqueSlug(String name, Long excludingProductId) {
        String baseSlug = slugify(name);
        String candidate = baseSlug;
        int suffix = 2;

        while (excludingProductId == null
                ? marketplaceProductRepository.existsBySlug(candidate)
                : marketplaceProductRepository.existsBySlugAndIdNot(candidate, excludingProductId)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String slugify(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return "product";
        }

        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (ascii.isBlank()) {
            return "product";
        }
        return ascii;
    }

    private void validateFarmerProductStatusTransition(
            MarketplaceProductStatus current,
            MarketplaceProductStatus target) {
        if (current == target) {
            return; // no-op
        }

        boolean allowed = switch (current) {
            case DRAFT -> target == MarketplaceProductStatus.PENDING_REVIEW;
            case ACTIVE -> target == MarketplaceProductStatus.INACTIVE || target == MarketplaceProductStatus.SOLD_OUT;
            case INACTIVE -> target == MarketplaceProductStatus.ACTIVE;
            // Legacy support for backward compatibility with existing data
            case PUBLISHED -> target == MarketplaceProductStatus.HIDDEN;
            case HIDDEN -> target == MarketplaceProductStatus.PENDING_REVIEW;
            default -> false;
        };

        if (!allowed) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateAdminProductStatusTransition(
            MarketplaceProductStatus current,
            MarketplaceProductStatus target) {
        if (current == target) {
            return; // no-op
        }

        // Admin has full control over status transitions
        // All transitions are allowed for admin users
        // This provides flexibility for moderation and emergency actions
    }

    private void validateFarmerOrderStatusTransition(
            MarketplaceOrderStatus current,
            MarketplaceOrderStatus target) {
        if (current == target) {
            return;
        }
        boolean valid = switch (current) {
            case PENDING_PAYMENT -> target == MarketplaceOrderStatus.REJECTED
                    || target == MarketplaceOrderStatus.CANCELLED;
            case PAYMENT_SUBMITTED -> target == MarketplaceOrderStatus.REJECTED
                    || target == MarketplaceOrderStatus.CANCELLED;
            case PAYMENT_VERIFIED -> target == MarketplaceOrderStatus.CONFIRMED
                    || target == MarketplaceOrderStatus.REJECTED;
            case CONFIRMED -> target == MarketplaceOrderStatus.PREPARING
                    || target == MarketplaceOrderStatus.CANCELLED;
            case PREPARING -> target == MarketplaceOrderStatus.SHIPPED;
            case SHIPPED -> target == MarketplaceOrderStatus.DELIVERED;
            case DELIVERED -> target == MarketplaceOrderStatus.COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };
        if (!valid) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeAddressLabel(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return "home";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if ("home".equals(lower) || "office".equals(lower) || "other".equals(lower)) {
            return lower;
        }
        return "other";
    }

    private String generateOrderGroupCode() {
        return "MOG-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String generateOrderCode() {
        return "MO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private record ShippingSnapshot(String recipientName, String phone, String addressLine) {
    }
}
