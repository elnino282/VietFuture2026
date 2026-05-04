package org.example.QuanLyMuaVu.module.inventory.repository;


import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductWarehouseTransactionRepository extends JpaRepository<ProductWarehouseTransaction, Integer> {

    @Query("""
            select t from ProductWarehouseTransaction t
            where t.lot.farm.id in :farmIds
              and (:lotId is null or t.lot.id = :lotId)
              and (:type is null or t.transactionType = :type)
              and (:fromDate is null or t.createdAt >= :fromDate)
              and (:toDate is null or t.createdAt <= :toDate)
            """)
    Page<ProductWarehouseTransaction> searchTransactions(
            @Param("farmIds") List<Integer> farmIds,
            @Param("lotId") Integer lotId,
            @Param("type") ProductWarehouseTransactionType type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    List<ProductWarehouseTransaction> findAllByLot_IdOrderByCreatedAtDesc(Integer lotId);
}

