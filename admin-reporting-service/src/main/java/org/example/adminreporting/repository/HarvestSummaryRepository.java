package org.example.adminreporting.repository;

import org.example.adminreporting.entity.HarvestSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HarvestSummaryRepository extends JpaRepository<HarvestSummary, Integer> {
}
