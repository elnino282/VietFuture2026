package org.example.adminreporting.repository;

import java.util.List;
import org.example.adminreporting.entity.MarketplaceOrderSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceOrderSummaryRepository extends JpaRepository<MarketplaceOrderSummary, Long> {
    List<MarketplaceOrderSummary> findByPaymentStatus(String paymentStatus, Pageable pageable);
}
