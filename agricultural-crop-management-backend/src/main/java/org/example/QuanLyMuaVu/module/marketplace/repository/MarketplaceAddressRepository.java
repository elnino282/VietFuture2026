package org.example.QuanLyMuaVu.module.marketplace.repository;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceAddressRepository extends JpaRepository<MarketplaceAddress, Long> {

    List<MarketplaceAddress> findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(Long userId);

    Optional<MarketplaceAddress> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    Optional<MarketplaceAddress> findFirstByUser_IdAndIsDefaultTrueAndDeletedAtIsNullOrderByIdDesc(Long userId);

    boolean existsByUser_IdAndDeletedAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE MarketplaceAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    int clearDefaultByUserId(@Param("userId") Long userId);
}
