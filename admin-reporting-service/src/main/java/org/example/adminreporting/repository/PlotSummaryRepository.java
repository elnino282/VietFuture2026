package org.example.adminreporting.repository;

import org.example.adminreporting.entity.PlotSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlotSummaryRepository extends JpaRepository<PlotSummary, Integer> {
}
