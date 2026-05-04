package org.example.QuanLyMuaVu.module.marketplace.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceCartRepository extends JpaRepository<MarketplaceCart, Long> {

    Optional<MarketplaceCart> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM MarketplaceCart c WHERE c.user.id = :userId")
    Optional<MarketplaceCart> findByUserIdForUpdate(@Param("userId") Long userId);
}
