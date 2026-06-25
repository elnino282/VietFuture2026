package org.example.inventory.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.inventory.entity.InventoryBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    boolean existsByWarehouseId(Integer warehouseId);
    boolean existsBySupplyLotId(Integer supplyLotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLotId = :lotId
              AND ib.warehouseId = :warehouseId
              AND ((:locationId IS NULL AND ib.locationId IS NULL) OR ib.locationId = :locationId)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocationWithLock(
            @Param("lotId") Integer lotId,
            @Param("warehouseId") Integer warehouseId,
            @Param("locationId") Integer locationId);

    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLotId = :lotId
              AND ib.warehouseId = :warehouseId
              AND ((:locationId IS NULL AND ib.locationId IS NULL) OR ib.locationId = :locationId)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocation(
            @Param("lotId") Integer lotId,
            @Param("warehouseId") Integer warehouseId,
            @Param("locationId") Integer locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLotId = :lotId
              AND ib.warehouseId = :warehouseId
              AND ib.quantity > 0
            ORDER BY ib.id
            """)
    List<InventoryBalance> findAllPositiveByLotAndWarehouseWithLock(
            @Param("lotId") Integer lotId,
            @Param("warehouseId") Integer warehouseId);

    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.warehouseId = :warehouseId
              AND (:locationId IS NULL OR ib.locationId = :locationId)
              AND (:lotId IS NULL OR ib.supplyLotId = :lotId)
              AND ib.quantity > 0
              AND (
                    :q IS NULL
                    OR EXISTS (
                        SELECT 1 FROM SupplyLot lot, SupplyItem item
                        WHERE lot.id = ib.supplyLotId
                          AND item.id = lot.supplyItemId
                          AND (
                              LOWER(lot.batchCode) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(item.name) LIKE LOWER(CONCAT('%', :q, '%'))
                          )
                    )
              )
            """)
    Page<InventoryBalance> searchOnHand(
            @Param("warehouseId") Integer warehouseId,
            @Param("locationId") Integer locationId,
            @Param("lotId") Integer lotId,
            @Param("q") String q,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE InventoryBalance ib
            SET ib.quantity = ib.quantity - :qty
            WHERE ib.id = :id AND ib.quantity >= :qty
            """)
    int atomicDeduct(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
