package org.example.marketplace.repository;

import java.util.List;
import java.util.Optional;
import org.example.marketplace.entity.MarketplaceProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceProductReviewRepository extends JpaRepository<MarketplaceProductReview, Long> {
    Page<MarketplaceProductReview> findByProductId(Long productId, Pageable pageable);
    Page<MarketplaceProductReview> findByFarmId(Integer farmId, Pageable pageable);
    List<MarketplaceProductReview> findByProductIdIn(List<Long> productIds);
    Optional<MarketplaceProductReview> findByOrderItemIdAndBuyerUserId(Long orderItemId, Long buyerUserId);
}
