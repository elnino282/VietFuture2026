package org.example.inventory.repository;

import java.util.List;
import org.example.inventory.entity.ProductWarehouseTransaction;
import org.example.inventory.enums.ProductWarehouseTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductWarehouseTransactionRepository extends JpaRepository<ProductWarehouseTransaction, Integer> {
    List<ProductWarehouseTransaction> findAllByLotIdOrderByCreatedAtDesc(Integer lotId);

    @Query("""
            SELECT tx FROM ProductWarehouseTransaction tx
            WHERE (:lotId IS NULL OR tx.lotId = :lotId)
              AND (:type IS NULL OR tx.transactionType = :type)
              AND (:fromDate IS NULL OR tx.createdAt >= :fromDate)
              AND (:toDate IS NULL OR tx.createdAt <= :toDate)
            ORDER BY tx.createdAt DESC
            """)
    Page<ProductWarehouseTransaction> searchTransactions(
            @Param("lotId") Integer lotId,
            @Param("type") ProductWarehouseTransactionType type,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable);
}
