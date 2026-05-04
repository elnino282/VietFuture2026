package org.example.QuanLyMuaVu.module.marketplace.repository;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceOrderItemRepository extends JpaRepository<MarketplaceOrderItem, Long> {

    boolean existsByOrder_IdAndProduct_Id(Long orderId, Long productId);

    List<MarketplaceOrderItem> findAllByOrder_IdIn(List<Long> orderIds);

    Optional<MarketplaceOrderItem> findByIdAndOrder_Id(Long id, Long orderId);
}
