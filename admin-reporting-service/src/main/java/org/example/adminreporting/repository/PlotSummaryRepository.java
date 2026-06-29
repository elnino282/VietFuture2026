package org.example.adminreporting.repository;

import org.example.adminreporting.entity.PlotSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlotSummaryRepository extends JpaRepository<PlotSummary, Integer> {
    Page<PlotSummary> findByFarmId(Integer farmId, Pageable pageable);
    Page<PlotSummary> findByPlotNameContainingIgnoreCase(String keyword, Pageable pageable);
    Page<PlotSummary> findByFarmIdAndPlotNameContainingIgnoreCase(Integer farmId, String keyword, Pageable pageable);
}
