package org.example.adminreporting.repository;

import org.example.adminreporting.entity.AlertSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertSummaryRepository extends JpaRepository<AlertSummary, Integer> {
    long countBySeasonIdAndStatusAndSeverity(Integer seasonId, String status, String severity);
}
