package org.example.adminreporting.repository;

import org.example.adminreporting.entity.IncidentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentSummaryRepository extends JpaRepository<IncidentSummary, Integer> {
    long countBySeasonIdAndStatus(Integer seasonId, String status);
    Page<IncidentSummary> findByStatus(String status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM IncidentSummary i " +
           "LEFT JOIN SeasonSummary s ON i.seasonId = s.seasonId " +
           "WHERE (:year IS NULL OR YEAR(s.startDate) = :year)")
    java.util.List<IncidentSummary> findIncidentsByYear(@org.springframework.data.repository.query.Param("year") Integer year);
}
