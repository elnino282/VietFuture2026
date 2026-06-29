package org.example.adminreporting.repository;

import org.example.adminreporting.entity.TaskSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskSummaryRepository extends JpaRepository<TaskSummary, Integer> {
    long countBySeasonIdAndStatus(Integer seasonId, String status);
}
