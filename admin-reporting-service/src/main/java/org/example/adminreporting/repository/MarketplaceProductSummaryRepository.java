package org.example.adminreporting.repository;

import java.util.List;
import org.example.adminreporting.entity.MarketplaceProductSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceProductSummaryRepository extends JpaRepository<MarketplaceProductSummary, Long> {
    List<MarketplaceProductSummary> findByStatus(String status, Pageable pageable);
}
