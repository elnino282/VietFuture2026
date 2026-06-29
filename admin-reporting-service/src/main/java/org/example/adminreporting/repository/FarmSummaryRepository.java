package org.example.adminreporting.repository;

import org.example.adminreporting.entity.FarmSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmSummaryRepository extends JpaRepository<FarmSummary, Integer> {
}
