package org.example.adminreporting.repository;

import org.example.adminreporting.entity.InventoryLotSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.example.adminreporting.dto.response.DashboardStatsDTO.InventoryHealth;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface InventoryLotSummaryRepository extends JpaRepository<InventoryLotSummary, Integer> {
    long countByFarmId(Integer farmId);

    @Query("SELECT new org.example.adminreporting.dto.response.DashboardStatsDTO$InventoryHealth(" +
           "lot.farmId, lot.farmName, " +
           "SUM(CASE WHEN lot.expiryDate < :today THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN lot.expiryDate >= :today AND lot.expiryDate <= :cutoff THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN lot.expiryDate <= :cutoff THEN 1L ELSE 0L END)) " +
           "FROM InventoryLotSummary lot " +
           "WHERE lot.expiryDate IS NOT NULL AND lot.expiryDate <= :cutoff " +
           "GROUP BY lot.farmId, lot.farmName")
    List<InventoryHealth> findInventoryHealth(@Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);
}
