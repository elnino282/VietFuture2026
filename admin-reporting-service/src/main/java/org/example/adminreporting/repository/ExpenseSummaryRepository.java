package org.example.adminreporting.repository;

import org.example.adminreporting.entity.ExpenseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExpenseSummaryRepository extends JpaRepository<ExpenseSummary, Integer> {
    List<ExpenseSummary> findBySeasonIdIn(Collection<Integer> seasonIds);
}
