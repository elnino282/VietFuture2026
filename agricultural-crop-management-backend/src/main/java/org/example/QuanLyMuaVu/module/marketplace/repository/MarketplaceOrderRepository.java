package org.example.QuanLyMuaVu.module.marketplace.repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceOrderStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    Page<MarketplaceOrder> findByPaymentVerificationStatus(
            MarketplacePaymentVerificationStatus paymentVerificationStatus, Pageable pageable);

    Page<MarketplaceOrder> findByBuyerUser_Id(Long buyerUserId, Pageable pageable);

    Page<MarketplaceOrder> findByBuyerUser_IdAndStatus(Long buyerUserId, MarketplaceOrderStatus status, Pageable pageable);

    Page<MarketplaceOrder> findByFarmerUser_Id(Long farmerUserId, Pageable pageable);

    Page<MarketplaceOrder> findByFarmerUser_IdAndStatus(Long farmerUserId, MarketplaceOrderStatus status, Pageable pageable);

    Page<MarketplaceOrder> findByStatus(MarketplaceOrderStatus status, Pageable pageable);

    long countByFarmerUser_IdAndStatus(Long farmerUserId, MarketplaceOrderStatus status);

    long countByStatus(MarketplaceOrderStatus status);

    long countByFarmerUser_Id(Long farmerUserId);

    long countByPaymentVerificationStatus(MarketplacePaymentVerificationStatus paymentVerificationStatus);

    @Query("""
            SELECT o.id FROM MarketplaceOrder o
            WHERE o.buyerUser.id = :buyerUserId
            """)
    Page<Long> findBuyerOrderIds(
            @Param("buyerUserId") Long buyerUserId,
            Pageable pageable);

    @Query("""
            SELECT o.id FROM MarketplaceOrder o
            WHERE o.buyerUser.id = :buyerUserId
              AND o.status = :status
            """)
    Page<Long> findBuyerOrderIdsByStatus(
            @Param("buyerUserId") Long buyerUserId,
            @Param("status") MarketplaceOrderStatus status,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o
            LEFT JOIN FETCH o.orderGroup
            LEFT JOIN FETCH o.buyerUser
            LEFT JOIN FETCH o.farmerUser
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id IN :orderIds
            """)
    List<MarketplaceOrder> findByIdsWithResponseGraph(@Param("orderIds") List<Long> orderIds);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o
            LEFT JOIN FETCH o.orderGroup
            LEFT JOIN FETCH o.buyerUser
            LEFT JOIN FETCH o.farmerUser
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id = :orderId AND o.buyerUser.id = :buyerUserId
            """)
    Optional<MarketplaceOrder> findByIdAndBuyerUserIdWithItems(
            @Param("orderId") Long orderId,
            @Param("buyerUserId") Long buyerUserId);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id = :orderId AND o.farmerUser.id = :farmerUserId
            """)
    Optional<MarketplaceOrder> findByIdAndFarmerUserIdWithItems(
            @Param("orderId") Long orderId,
            @Param("farmerUserId") Long farmerUserId);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.id = :orderId
            """)
    Optional<MarketplaceOrder> findByIdWithItems(@Param("orderId") Long orderId);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            WHERE o.orderGroup.id = :orderGroupId
            ORDER BY o.id ASC
            """)
    List<MarketplaceOrder> findAllByOrderGroupIdWithItems(@Param("orderGroupId") Long orderGroupId);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM MarketplaceOrder o
            WHERE o.farmerUser.id = :farmerUserId
              AND o.status = :status
            """)
    BigDecimal sumTotalAmountByFarmerUserIdAndStatus(
            @Param("farmerUserId") Long farmerUserId,
            @Param("status") MarketplaceOrderStatus status);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM MarketplaceOrder o
            WHERE o.status = :status
            """)
    BigDecimal sumTotalAmountByStatus(@Param("status") MarketplaceOrderStatus status);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE o.farmerUser.id = :farmerUserId
            ORDER BY o.createdAt DESC
            """)
    List<MarketplaceOrder> findRecentByFarmerUserId(
            @Param("farmerUserId") Long farmerUserId,
            Pageable pageable);

    @Query("""
            SELECT MAX(o.createdAt)
            FROM MarketplaceOrder o
            WHERE o.farmerUser.id = :farmerUserId
            """)
    LocalDateTime findLastOrderAtByFarmerUserId(@Param("farmerUserId") Long farmerUserId);

    @Query("""
            SELECT MAX(o.createdAt)
            FROM MarketplaceOrder o
            """)
    LocalDateTime findLastOrderAt();
}
