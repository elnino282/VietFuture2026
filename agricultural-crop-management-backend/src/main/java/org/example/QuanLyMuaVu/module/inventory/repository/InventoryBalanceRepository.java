package org.example.QuanLyMuaVu.module.inventory.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    boolean existsByWarehouse_Id(Integer warehouseId);

    boolean existsBySupplyLot_Id(Integer supplyLotId);

    boolean existsBySupplyLot_IdAndWarehouse_Farm_IdIn(Integer supplyLotId, List<Integer> farmIds);

    /**
     * Find inventory balance with pessimistic write lock for upsert operations.
     * Used for IN movements and positive ADJUST movements.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocationWithLock(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
            ORDER BY ib.id ASC
            """)
    List<InventoryBalance> findAllByLotAndWarehouseWithLock(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse);

    /**
     * Find inventory balance without lock (for read operations).
     */
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocation(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);

    /**
     * Atomic deduct operation with stock check.
     * Returns 1 if successful, 0 if insufficient stock.
     * Used for OUT movements and negative ADJUST movements.
     */
    @Modifying
    @Query("""
            UPDATE InventoryBalance ib
            SET ib.quantity = ib.quantity - :qty
            WHERE ib.id = :id AND ib.quantity >= :qty
            """)
    int atomicDeduct(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * Get current quantity for a supply lot at a warehouse/location.
     */
    @Query("""
            SELECT COALESCE(ib.quantity, 0)
            FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    BigDecimal getCurrentQuantity(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);

    @Query("""
            SELECT COALESCE(SUM(ib.quantity), 0)
            FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
            """)
    BigDecimal sumQuantityByLotAndWarehouse(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse);

    @Query("""
            SELECT ib FROM InventoryBalance ib
            JOIN FETCH ib.supplyLot sl
            JOIN FETCH sl.supplyItem
            LEFT JOIN FETCH sl.supplier
            JOIN FETCH ib.warehouse w
            JOIN FETCH w.farm
            LEFT JOIN FETCH ib.location
            """)
    List<InventoryBalance> findAllWithDetails();

    @Query("""
            SELECT ib FROM InventoryBalance ib
            JOIN FETCH ib.warehouse w
            JOIN FETCH w.farm
            LEFT JOIN FETCH ib.location
            WHERE ib.supplyLot.id = :lotId
            """)
    List<InventoryBalance> findDetailedBySupplyLotId(@Param("lotId") Integer lotId);
}
