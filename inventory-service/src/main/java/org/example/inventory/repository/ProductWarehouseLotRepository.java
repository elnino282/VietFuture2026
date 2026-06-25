package org.example.inventory.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.inventory.entity.ProductWarehouseLot;
import org.example.inventory.enums.ProductWarehouseLotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductWarehouseLotRepository extends JpaRepository<ProductWarehouseLot, Integer> {
    boolean existsByWarehouseId(Integer warehouseId);
    boolean existsByHarvestId(Integer harvestId);
    long countByStatus(ProductWarehouseLotStatus status);
    Optional<ProductWarehouseLot> findByHarvestId(Integer harvestId);
    List<ProductWarehouseLot> findAllByHarvestIdIn(Collection<Integer> harvestIds);
    Optional<ProductWarehouseLot> findByLotCode(String lotCode);
    List<ProductWarehouseLot> findAllBySeasonIdIn(List<Integer> seasonIds);

    @Query("SELECT COALESCE(SUM(l.onHandQuantity), 0) FROM ProductWarehouseLot l")
    BigDecimal sumOnHandQuantity();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ProductWarehouseLot l WHERE l.id = :id")
    Optional<ProductWarehouseLot> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select l from ProductWarehouseLot l
            where l.farmId in :farmIds
              and (:warehouseId is null or l.warehouseId = :warehouseId)
              and (:locationId is null or l.locationId = :locationId)
              and (:seasonId is null or l.seasonId = :seasonId)
              and (:farmId is null or l.farmId = :farmId)
              and (:plotId is null or l.plotId = :plotId)
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

    @Query("""
            select l from ProductWarehouseLot l
            where (:warehouseId is null or l.warehouseId = :warehouseId)
              and (:locationId is null or l.locationId = :locationId)
              and (:seasonId is null or l.seasonId = :seasonId)
              and (:farmId is null or l.farmId = :farmId)
              and (:plotId is null or l.plotId = :plotId)
              and (:harvestedFrom is null or l.harvestedAt >= :harvestedFrom)
              and (:harvestedTo is null or l.harvestedAt <= :harvestedTo)
              and (:status is null or l.status = :status)
              and (:q is null
                   or lower(l.lotCode) like lower(concat('%', :q, '%'))
                   or lower(l.productName) like lower(concat('%', :q, '%')))
            """)
    Page<ProductWarehouseLot> searchLotsPublic(
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

    @Query("""
            SELECT l FROM ProductWarehouseLot l
            WHERE l.farmId = :farmId
              AND l.warehouseId = :warehouseId
              AND LOWER(l.productName) = LOWER(:productName)
              AND l.lotCode = :lotCode
            """)
    List<ProductWarehouseLot> findByFarmWarehouseProductAndLotCode(
            @Param("farmId") Integer farmId,
            @Param("warehouseId") Integer warehouseId,
            @Param("productName") String productName,
            @Param("lotCode") String lotCode);
}
