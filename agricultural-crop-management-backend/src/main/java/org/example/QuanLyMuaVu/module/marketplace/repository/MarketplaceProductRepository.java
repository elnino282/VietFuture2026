package org.example.QuanLyMuaVu.module.marketplace.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceProductRepository extends JpaRepository<MarketplaceProduct, Long>, JpaSpecificationExecutor<MarketplaceProduct> {

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            WHERE p.status IN :statuses
              AND p.stockQuantity > 0
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:traceable IS NULL OR p.traceable = :traceable)
              AND (:region IS NULL OR LOWER(COALESCE(p.farmRegion, '')) LIKE LOWER(CONCAT('%', :region, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:farmId IS NULL OR p.farmId = :farmId)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            WHERE p.status IN :statuses
              AND p.stockQuantity > 0
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:traceable IS NULL OR p.traceable = :traceable)
              AND (:region IS NULL OR LOWER(COALESCE(p.farmRegion, '')) LIKE LOWER(CONCAT('%', :region, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:farmId IS NULL OR p.farmId = :farmId)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchPublished(
            @Param("statuses") Collection<MarketplaceProductStatus> statuses,
            @Param("category") String category,
            @Param("q") String q,
            @Param("traceable") Boolean traceable,
            @Param("region") String region,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("farmId") Integer farmId,
            Pageable pageable);

    @Query("""
            SELECT p FROM MarketplaceProduct p
            WHERE p.slug = :slug
              AND p.status IN :statuses
              AND p.stockQuantity > 0
            """)
    Optional<MarketplaceProduct> findSellableBySlugAndStatusIn(
            @Param("slug") String slug,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses);

    @Query("""
            SELECT p FROM MarketplaceProduct p
            WHERE p.id = :id
              AND p.status IN :statuses
              AND p.stockQuantity > 0
            """)
    Optional<MarketplaceProduct> findSellableByIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses);

    default Optional<MarketplaceProduct> findSellableByIdAndStatus(Long id, MarketplaceProductStatus status) {
        return findSellableByIdAndStatusIn(id, List.of(status));
    }

    @Query("""
            SELECT p FROM MarketplaceProduct p
            WHERE p.id = :id
            """)
    Optional<MarketplaceProduct> findByIdWithLotForCartValidation(@Param("id") Long id);

    Optional<MarketplaceProduct> findByIdAndStatus(Long id, MarketplaceProductStatus status);

    Optional<MarketplaceProduct> findByIdAndFarmerUserId(Long id, Long farmerUserId);

    Optional<MarketplaceProduct> findByLotId(Integer lotId);

    List<MarketplaceProduct> findAllByLotIdIn(Collection<Integer> lotIds);

    boolean existsByLotId(Integer lotId);

    boolean existsByLotIdAndIdNot(Integer lotId, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByFarmerUserId(Long farmerUserId);

    long countByFarmerUserIdAndStatus(Long farmerUserId, MarketplaceProductStatus status);

    long countByFarmerUserIdAndStatusIn(Long farmerUserId, Collection<MarketplaceProductStatus> statuses);

    long countByStatus(MarketplaceProductStatus status);

    long countByStatusIn(Collection<MarketplaceProductStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MarketplaceProduct p WHERE p.id IN :ids")
    List<MarketplaceProduct> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT COUNT(p)
            FROM MarketplaceProduct p
            WHERE p.farmId = :farmId
              AND p.status IN :statuses
              AND p.stockQuantity > 0
            """)
    long countSellableByFarmIdAndStatusIn(
            @Param("farmId") Integer farmId,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses);

    @Query("""
            SELECT COUNT(p) > 0
            FROM MarketplaceProduct p
            WHERE p.farmId = :farmId
              AND p.status IN :statuses
              AND p.traceable = true
              AND p.stockQuantity > 0
            """)
    boolean existsSellableTraceableByFarmIdAndStatusIn(
            @Param("farmId") Integer farmId,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses);

    @Query(value = """
            SELECT DISTINCT p.farmId AS farmId, p.farmName AS farmName
            FROM MarketplaceProduct p
            WHERE p.status IN :statuses
              AND p.stockQuantity > 0
              AND (:q IS NULL OR LOWER(p.farmName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:region IS NULL OR LOWER(COALESCE(p.farmRegion, '')) LIKE LOWER(CONCAT('%', :region, '%')))
            ORDER BY p.farmName ASC, p.farmId ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT p.farmId) FROM MarketplaceProduct p
            WHERE p.status IN :statuses
              AND p.stockQuantity > 0
              AND (:q IS NULL OR LOWER(p.farmName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:region IS NULL OR LOWER(COALESCE(p.farmRegion, '')) LIKE LOWER(CONCAT('%', :region, '%')))
            """)
    Page<FarmProjection> searchDistinctFarmsWithPublishedProducts(
            @Param("statuses") Collection<MarketplaceProductStatus> statuses,
            @Param("q") String q,
            @Param("region") String region,
            Pageable pageable);

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            WHERE p.farmerUserId = :farmerUserId
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            WHERE p.farmerUserId = :farmerUserId
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchFarmerProducts(
            @Param("farmerUserId") Long farmerUserId,
            @Param("status") MarketplaceProductStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            WHERE (:status IS NULL OR p.status = :status)
              AND ((p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.ACTIVE
                    AND p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.PUBLISHED)
                   OR (p.stockQuantity > 0))
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmerDisplayName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            WHERE (:status IS NULL OR p.status = :status)
              AND ((p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.ACTIVE
                    AND p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.PUBLISHED)
                   OR (p.stockQuantity > 0))
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.farmerDisplayName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchAdminProducts(
            @Param("status") MarketplaceProductStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT p.farmId AS farmId, COUNT(p.id) AS productCount
            FROM MarketplaceProduct p
            WHERE p.status IN :statuses
              AND p.farmId IN :farmIds
              AND p.stockQuantity > 0
            GROUP BY p.farmId
            """)
    List<FarmProductCountProjection> countPublishedByFarmIds(
            @Param("farmIds") Collection<Integer> farmIds,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses);

    @Query("""
            SELECT p
            FROM MarketplaceProduct p
            WHERE p.farmId = :farmId
              AND p.status IN :statuses
              AND p.stockQuantity > 0
            ORDER BY p.publishedAt DESC, p.id DESC
            """)
    List<MarketplaceProduct> findSellableByFarmIdAndStatusInOrderByPublishedAtDescIdDesc(
            @Param("farmId") Integer farmId,
            @Param("statuses") Collection<MarketplaceProductStatus> statuses,
            Pageable pageable);

    interface FarmProductCountProjection {
        Integer getFarmId();

        Long getProductCount();
    }

    interface FarmProjection {
        Integer getFarmId();

        String getFarmName();
    }
}
