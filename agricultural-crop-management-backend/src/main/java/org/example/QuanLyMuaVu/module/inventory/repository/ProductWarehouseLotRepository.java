package org.example.QuanLyMuaVu.module.inventory.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductWarehouseLotRepository extends JpaRepository<ProductWarehouseLot, Integer> {

    boolean existsByWarehouse_Id(Integer warehouseId);

    boolean existsByHarvest_Id(Integer harvestId);

    Optional<ProductWarehouseLot> findByHarvest_Id(Integer harvestId);

    Optional<ProductWarehouseLot> findByLotCode(String lotCode);

    Optional<ProductWarehouseLot> findByIdAndStatus(Integer id, ProductWarehouseLotStatus status);

    Optional<ProductWarehouseLot> findByIdAndOnHandQuantityGreaterThan(Integer id, BigDecimal onHandQuantity);

    @Query("""
            SELECT l FROM ProductWarehouseLot l
            WHERE l.id = :lotId
              AND l.farm.user.id = :ownerId
            """)
    Optional<ProductWarehouseLot> findByIdAndFarmUserId(
            @Param("lotId") Integer lotId,
            @Param("ownerId") Long ownerId);

    Optional<ProductWarehouseLot> findByIdAndFarmUserIdAndStatus(Integer lotId, Long ownerId, ProductWarehouseLotStatus status);

    Optional<ProductWarehouseLot> findByIdAndFarmUserIdAndStatusAndOnHandQuantityGreaterThan(
            Integer lotId,
            Long ownerId,
            ProductWarehouseLotStatus status,
            BigDecimal onHandQuantity);

    @Query("""
            SELECT l FROM ProductWarehouseLot l
            LEFT JOIN FETCH l.farm f
            LEFT JOIN FETCH l.season s
            WHERE f.user.id = :ownerId
              AND l.status = org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus.IN_STOCK
              AND l.onHandQuantity > 0
            ORDER BY l.harvestedAt DESC, l.id DESC
            """)
    List<ProductWarehouseLot> findSellableByFarmUserId(@Param("ownerId") Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ProductWarehouseLot l WHERE l.id IN :ids")
    List<ProductWarehouseLot> findAllByIdInForUpdate(@Param("ids") Collection<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ProductWarehouseLot l WHERE l.id = :id")
    Optional<ProductWarehouseLot> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select l from ProductWarehouseLot l
            where l.farm.id = :farmId
              and l.warehouse.id = :warehouseId
              and lower(l.productName) = lower(:productName)
              and l.lotCode = :lotCode
            """)
    List<ProductWarehouseLot> findByFarmWarehouseProductAndLotCode(
            @Param("farmId") Integer farmId,
            @Param("warehouseId") Integer warehouseId,
            @Param("productName") String productName,
            @Param("lotCode") String lotCode);

    long countByFarm_IdIn(List<Integer> farmIds);

    @Query("select coalesce(sum(l.onHandQuantity), 0) from ProductWarehouseLot l where l.farm.id in :farmIds")
    BigDecimal sumOnHandByFarmIds(@Param("farmIds") List<Integer> farmIds);

    @Query("select count(l) from ProductWarehouseLot l where l.farm.id in :farmIds and l.onHandQuantity > 0")
    long countByFarmIdsAndOnHandPositive(@Param("farmIds") List<Integer> farmIds);

    @Query("select count(l) from ProductWarehouseLot l where l.farm.id in :farmIds and l.onHandQuantity <= 0")
    long countByFarmIdsAndOnHandDepleted(@Param("farmIds") List<Integer> farmIds);

    @Query("""
            select count(p)
            from MarketplaceProduct p
            join p.lot l
            where p.farmerUser.id = :farmerUserId
              and p.status = :status
              and p.stockQuantity > 0
              and l.onHandQuantity > 0
              and (case when p.stockQuantity < l.onHandQuantity then p.stockQuantity else l.onHandQuantity end) <= :threshold
            """)
    long countMarketplaceListingsByFarmerAndOnHandLessThanEqual(
            @Param("farmerUserId") Long farmerUserId,
            @Param("status") MarketplaceProductStatus status,
            @Param("threshold") BigDecimal threshold);

    @Query("""
            select l from ProductWarehouseLot l
            where l.farm.id in :farmIds
              and (:warehouseId is null or l.warehouse.id = :warehouseId)
              and (:locationId is null or l.location.id = :locationId)
              and (:seasonId is null or l.season.id = :seasonId)
              and (:farmId is null or l.farm.id = :farmId)
              and (:plotId is null or l.plot.id = :plotId)
              and (:harvestedFrom is null or l.harvestedAt >= :harvestedFrom)
              and (:harvestedTo is null or l.harvestedAt <= :harvestedTo)
              and (:status is null or l.status = :status)
              and (:q is null
                   or lower(l.lotCode) like lower(concat('%', :q, '%'))
                   or lower(l.productName) like lower(concat('%', :q, '%')))
            """)
    Page<ProductWarehouseLot> searchLots(
            @Param("farmIds") List<Integer> farmIds,
            @Param("warehouseId") Integer warehouseId,
            @Param("locationId") Integer locationId,
            @Param("seasonId") Integer seasonId,
            @Param("farmId") Integer farmId,
            @Param("plotId") Integer plotId,
            @Param("harvestedFrom") LocalDate harvestedFrom,
            @Param("harvestedTo") LocalDate harvestedTo,
            @Param("status") ProductWarehouseLotStatus status,
            @Param("q") String q,
            Pageable pageable);
}
