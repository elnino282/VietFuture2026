package org.example.adminreporting.repository;

import org.example.adminreporting.entity.FarmSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmSummaryRepository extends JpaRepository<FarmSummary, Integer> {
    Page<FarmSummary> findByFarmNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT COUNT(f) FROM FarmSummary f WHERE f.active = true")
    long countActiveFarms();

    @Query("SELECT COUNT(f) FROM FarmSummary f WHERE f.active = false")
    long countInactiveFarms();
}
