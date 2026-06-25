package org.example.season.repository;

import java.util.List;
import org.example.season.entity.TaskProgressLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskProgressLogRepository extends JpaRepository<TaskProgressLog, Integer> {

    @Query("SELECT l FROM TaskProgressLog l " +
            "WHERE l.task.season.id = :seasonId " +
            "AND (:employeeUserId IS NULL OR l.employeeUserId = :employeeUserId) " +
            "AND (:taskId IS NULL OR l.task.id = :taskId) " +
            "ORDER BY l.loggedAt DESC, l.id DESC")
    Page<TaskProgressLog> findBySeasonFilters(@Param("seasonId") Integer seasonId,
            @Param("employeeUserId") Long employeeUserId,
            @Param("taskId") Integer taskId,
            Pageable pageable);

    @Query("SELECT l FROM TaskProgressLog l WHERE l.employeeUserId = :employeeUserId ORDER BY l.loggedAt DESC, l.id DESC")
    Page<TaskProgressLog> findByEmployeeUserId(@Param("employeeUserId") Long employeeUserId, Pageable pageable);

    @Query("""
            SELECT l FROM TaskProgressLog l
            JOIN FETCH l.task t
            WHERE t.userId = :ownerId
            ORDER BY l.loggedAt DESC, l.id DESC
            """)
    List<TaskProgressLog> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
