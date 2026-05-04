package org.example.QuanLyMuaVu.module.inventory.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.StockMovement;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

        boolean existsByWarehouse_Id(Integer warehouseId);

        boolean existsBySupplyLot_Id(Integer supplyLotId);

        boolean existsBySupplyLot_IdAndWarehouse_Farm_IdIn(Integer supplyLotId, List<Integer> farmIds);

        @Query("""
                        select coalesce(sum(
                            case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                 when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                 else m.quantity end
                        ), 0)
                        from StockMovement m
                        where m.supplyLot = :lot
                          and m.warehouse = :warehouse
                          and (:location is null or m.location = :location)
                        """)
        BigDecimal calculateOnHandQuantity(
                        @Param("lot") SupplyLot lot,
                        @Param("warehouse") Warehouse warehouse,
                        @Param("location") StockLocation location);

        /**
         * Find distinct supply lot IDs with movements at a warehouse/location
         */
        @Query("""
                        select distinct m.supplyLot.id
                        from StockMovement m
                        where m.warehouse = :warehouse
                          and (:location is null or m.location = :location)
                        """)
        List<Integer> findDistinctSupplyLotIdsByWarehouse(
                        @Param("warehouse") Warehouse warehouse,
                        @Param("location") StockLocation location);

        /**
         * Paginated movement history with filters
         */
        @Query("""
                        select m from StockMovement m
                        where m.warehouse = :warehouse
                          and (:type is null or m.movementType = :type)
                          and (:from is null or m.movementDate >= :from)
                          and (:to is null or m.movementDate <= :to)
                        order by m.movementDate desc
                        """)
        Page<StockMovement> findByWarehouseWithFilters(
                        @Param("warehouse") Warehouse warehouse,
                        @Param("type") StockMovementType type,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        Pageable pageable);

        @Query("""
                        select m from StockMovement m
                        left join fetch m.season
                        left join fetch m.task
                        where m.supplyLot.id = :lotId
                        order by m.movementDate desc
                        """)
        List<StockMovement> findBySupplyLotIdOrderByMovementDateDesc(@Param("lotId") Integer lotId);

        /**
         * Find all movements for a warehouse (simpler version)
         */
        Page<StockMovement> findByWarehouseOrderByMovementDateDesc(Warehouse warehouse, Pageable pageable);
}
