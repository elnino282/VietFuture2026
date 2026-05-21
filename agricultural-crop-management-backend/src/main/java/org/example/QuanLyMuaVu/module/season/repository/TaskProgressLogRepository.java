package org.example.QuanLyMuaVu.module.season.repository;



import java.util.List;
import org.example.QuanLyMuaVu.module.season.entity.TaskProgressLog;
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
            "AND (:employeeId IS NULL OR l.employee.id = :employeeId) " +
            "AND (:taskId IS NULL OR l.task.id = :taskId) " +
            "ORDER BY l.loggedAt DESC, l.id DESC")
    Page<TaskProgressLog> findBySeasonFilters(@Param("seasonId") Integer seasonId,
            @Param("employeeId") Long employeeId,
            @Param("taskId") Integer taskId,
            Pageable pageable);

    @Query("SELECT l FROM TaskProgressLog l WHERE l.employee.id = :employeeId ORDER BY l.loggedAt DESC, l.id DESC")
    Page<TaskProgressLog> findByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("""
            SELECT l FROM TaskProgressLog l
            LEFT JOIN FETCH l.task t
            LEFT JOIN FETCH t.season s
            LEFT JOIN FETCH s.plot p
            LEFT JOIN FETCH l.employee e
            WHERE t.user.id = :ownerId
            ORDER BY l.loggedAt DESC, l.id DESC
            """)
    List<TaskProgressLog> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
