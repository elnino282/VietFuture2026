package org.example.marketplace.repository;

import org.example.marketplace.entity.MarketplaceOrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceOrderAuditLogRepository extends JpaRepository<MarketplaceOrderAuditLog, Long> {
    List<MarketplaceOrderAuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, Long entityId);
}
