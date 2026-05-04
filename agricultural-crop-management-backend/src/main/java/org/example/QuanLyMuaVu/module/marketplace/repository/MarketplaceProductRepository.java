package org.example.QuanLyMuaVu.module.marketplace.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceProductRepository extends JpaRepository<MarketplaceProduct, Long> {

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            JOIN p.lot lot
            LEFT JOIN p.farm f
            LEFT JOIN f.province province
            WHERE p.status = :status
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND p.stockQuantity > 0
              AND lot.onHandQuantity > 0
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:traceable IS NULL OR p.traceable = :traceable)
              AND (:region IS NULL OR LOWER(COALESCE(province.name, '')) LIKE LOWER(CONCAT('%', :region, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            JOIN p.lot lot
            LEFT JOIN p.farm f
            LEFT JOIN f.province province
            WHERE p.status = :status
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND p.stockQuantity > 0
              AND lot.onHandQuantity > 0
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:traceable IS NULL OR p.traceable = :traceable)
              AND (:region IS NULL OR LOWER(COALESCE(province.name, '')) LIKE LOWER(CONCAT('%', :region, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchPublished(
            @Param("status") MarketplaceProductStatus status,
            @Param("category") String category,
            @Param("q") String q,
            @Param("traceable") Boolean traceable,
            @Param("region") String region,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("""
            SELECT p FROM MarketplaceProduct p
            JOIN p.lot lot
            WHERE p.slug = :slug
              AND p.status = :status
              AND p.stockQuantity > 0
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
            """)
    Optional<MarketplaceProduct> findSellableBySlugAndStatus(
            @Param("slug") String slug,
            @Param("status") MarketplaceProductStatus status);

    @Query("""
            SELECT p FROM MarketplaceProduct p
            JOIN p.lot lot
            WHERE p.id = :id
              AND p.status = :status
              AND p.stockQuantity > 0
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
            """)
    Optional<MarketplaceProduct> findSellableByIdAndStatus(
            @Param("id") Long id,
            @Param("status") MarketplaceProductStatus status);

    Optional<MarketplaceProduct> findByIdAndStatus(Long id, MarketplaceProductStatus status);

    Optional<MarketplaceProduct> findByIdAndFarmerUser_Id(Long id, Long farmerUserId);

    Optional<MarketplaceProduct> findByLot_Id(Integer lotId);

    List<MarketplaceProduct> findAllByLot_IdIn(Collection<Integer> lotIds);

    boolean existsByLot_Id(Integer lotId);

    boolean existsByLot_IdAndIdNot(Integer lotId, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByFarmerUser_Id(Long farmerUserId);

    long countByFarmerUser_IdAndStatus(Long farmerUserId, MarketplaceProductStatus status);

    long countByStatus(MarketplaceProductStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MarketplaceProduct p WHERE p.id IN :ids")
    List<MarketplaceProduct> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT COUNT(p)
            FROM MarketplaceProduct p
            JOIN p.lot lot
            WHERE p.farm.id = :farmId
              AND p.status = :status
              AND p.stockQuantity > 0
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
            """)
    long countSellableByFarmIdAndStatus(
            @Param("farmId") Integer farmId,
            @Param("status") MarketplaceProductStatus status);

    @Query(value = """
            SELECT DISTINCT f FROM MarketplaceProduct p
            JOIN p.lot lot
            JOIN p.farm f
            LEFT JOIN f.province province
            WHERE p.status = :status
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
              AND (:q IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:region IS NULL OR LOWER(COALESCE(province.name, '')) LIKE LOWER(CONCAT('%', :region, '%')))
            """, countQuery = """
            SELECT COUNT(DISTINCT f.id) FROM MarketplaceProduct p
            JOIN p.lot lot
            JOIN p.farm f
            LEFT JOIN f.province province
            WHERE p.status = :status
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
              AND (:q IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:region IS NULL OR LOWER(COALESCE(province.name, '')) LIKE LOWER(CONCAT('%', :region, '%')))
            """)
    Page<Farm> searchDistinctFarmsWithPublishedProducts(
            @Param("status") MarketplaceProductStatus status,
            @Param("q") String q,
            @Param("region") String region,
            Pageable pageable);

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            LEFT JOIN p.farm f
            WHERE p.farmerUser.id = :farmerUserId
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            LEFT JOIN p.farm f
            WHERE p.farmerUser.id = :farmerUserId
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchFarmerProducts(
            @Param("farmerUserId") Long farmerUserId,
            @Param("status") MarketplaceProductStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query(value = """
            SELECT p FROM MarketplaceProduct p
            JOIN p.lot lot
            LEFT JOIN p.farm f
            LEFT JOIN p.farmerUser fu
            WHERE (:status IS NULL OR p.status = :status)
              AND (p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.PUBLISHED
                   OR (p.stockQuantity > 0
                       AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
                       AND lot.onHandQuantity > 0))
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM MarketplaceProduct p
            JOIN p.lot lot
            LEFT JOIN p.farm f
            LEFT JOIN p.farmerUser fu
            WHERE (:status IS NULL OR p.status = :status)
              AND (p.status <> org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus.PUBLISHED
                   OR (p.stockQuantity > 0
                       AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
                       AND lot.onHandQuantity > 0))
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MarketplaceProduct> searchAdminProducts(
            @Param("status") MarketplaceProductStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT p.farm.id AS farmId, COUNT(p.id) AS productCount
            FROM MarketplaceProduct p
            JOIN p.lot lot
            WHERE p.status = :status
              AND p.farm.id IN :farmIds
              AND p.stockQuantity > 0
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
            GROUP BY p.farm.id
            """)
    List<FarmProductCountProjection> countPublishedByFarmIds(
            @Param("farmIds") Collection<Integer> farmIds,
            @Param("status") MarketplaceProductStatus status);

    @Query("""
            SELECT p
            FROM MarketplaceProduct p
            JOIN p.lot lot
            WHERE p.farm.id = :farmId
              AND p.status = :status
              AND p.stockQuantity > 0
              AND lot.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND lot.onHandQuantity > 0
            ORDER BY p.publishedAt DESC, p.id DESC
            """)
    List<MarketplaceProduct> findSellableByFarmIdAndStatusOrderByPublishedAtDescIdDesc(
            @Param("farmId") Integer farmId,
            @Param("status") MarketplaceProductStatus status,
            Pageable pageable);

    interface FarmProductCountProjection {
        Integer getFarmId();

        Long getProductCount();
    }
}
