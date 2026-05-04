package org.example.QuanLyMuaVu.module.marketplace.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceCartItemRepository extends JpaRepository<MarketplaceCartItem, Long> {

    Optional<MarketplaceCartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);

    @Query("""
            SELECT ci FROM MarketplaceCartItem ci
            JOIN FETCH ci.product p
            LEFT JOIN FETCH p.farm
            LEFT JOIN FETCH p.season
            LEFT JOIN FETCH p.lot
            WHERE ci.cart.id = :cartId
            ORDER BY ci.id ASC
            """)
    List<MarketplaceCartItem> findByCartIdWithProduct(@Param("cartId") Long cartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ci FROM MarketplaceCartItem ci
            JOIN FETCH ci.product p
            LEFT JOIN FETCH p.farm
            LEFT JOIN FETCH p.season
            LEFT JOIN FETCH p.lot
            WHERE ci.cart.id = :cartId
            ORDER BY ci.id ASC
            """)
    List<MarketplaceCartItem> findByCartIdWithProductForUpdate(@Param("cartId") Long cartId);

    @Modifying
    @Query("DELETE FROM MarketplaceCartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    int deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM MarketplaceCartItem ci WHERE ci.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);
}
