package org.example.inventory.repository;

import java.util.List;
import org.example.inventory.enums.StockMovementType;
import org.example.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
    boolean existsByWarehouseId(Integer warehouseId);
    boolean existsBySupplyLotId(Integer supplyLotId);
    List<StockMovement> findBySupplyLotIdOrderByMovementDateDesc(@Param("lotId") Integer lotId);
    Page<StockMovement> findByWarehouseIdOrderByMovementDateDesc(Integer warehouseId, Pageable pageable);
    List<StockMovement> findAllBySeasonId(@Param("seasonId") Integer seasonId);

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE sm.warehouseId = :warehouseId
              AND (:type IS NULL OR sm.movementType = :type)
              AND (:fromDate IS NULL OR sm.movementDate >= :fromDate)
              AND (:toDate IS NULL OR sm.movementDate <= :toDate)
            ORDER BY sm.movementDate DESC
            """)
    Page<StockMovement> searchByWarehouse(
            @Param("warehouseId") Integer warehouseId,
            @Param("type") StockMovementType type,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable);
}
