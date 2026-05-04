package org.example.QuanLyMuaVu.module.marketplace.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceProductReviewRepository extends JpaRepository<MarketplaceProductReview, Long> {

    @Query("SELECT r FROM MarketplaceProductReview r WHERE r.product.id = :productId AND r.hidden = false ORDER BY r.createdAt DESC")
    Page<MarketplaceProductReview> findVisibleByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT r FROM MarketplaceProductReview r WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    Page<MarketplaceProductReview> findByProductId(@Param("productId") Long productId, Pageable pageable);

    boolean existsByProduct_IdAndOrder_IdAndBuyerUser_Id(Long productId, Long orderId, Long buyerUserId);

    boolean existsByOrderItem_IdAndBuyerUser_Id(Long orderItemId, Long buyerUserId);

    List<MarketplaceProductReview> findByOrder_IdAndBuyerUser_Id(Long orderId, Long buyerUserId);

    Optional<MarketplaceProductReview> findByProduct_IdAndOrder_IdAndBuyerUser_Id(Long productId, Long orderId, Long buyerUserId);

    @Query("""
            SELECT r.product.id AS productId,
                   COALESCE(AVG(r.rating), 0) AS averageRating,
                   COUNT(r.id) AS ratingCount
            FROM MarketplaceProductReview r
            WHERE r.product.id IN :productIds
              AND r.hidden = false
            GROUP BY r.product.id
            """)
    List<ProductRatingProjection> aggregateRatingsByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0) AS averageRating,
                   COUNT(r.id) AS ratingCount
            FROM MarketplaceProductReview r
            WHERE r.product.id = :productId
              AND r.hidden = false
            """)
    SingleProductRatingProjection aggregateRatingByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT r FROM MarketplaceProductReview r
            JOIN r.product p
            WHERE p.farm.id = :farmId
              AND r.hidden = false
            ORDER BY r.createdAt DESC
            """)
    Page<MarketplaceProductReview> findVisibleByFarmId(@Param("farmId") Integer farmId, Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0) AS averageRating,
                   COUNT(r.id) AS ratingCount
            FROM MarketplaceProductReview r
            JOIN r.product p
            WHERE p.farm.id = :farmId
              AND r.hidden = false
            """)
    SingleProductRatingProjection aggregateRatingByFarmId(@Param("farmId") Integer farmId);

    interface ProductRatingProjection {
        Long getProductId();

        Double getAverageRating();

        Long getRatingCount();
    }

    interface SingleProductRatingProjection {
        Double getAverageRating();

        Long getRatingCount();
    }
}
