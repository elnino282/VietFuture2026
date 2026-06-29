package org.example.adminreporting.repository;

import org.example.adminreporting.entity.IncidentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentSummaryRepository extends JpaRepository<IncidentSummary, Integer> {
    long countBySeasonIdAndStatus(Integer seasonId, String status);
}
