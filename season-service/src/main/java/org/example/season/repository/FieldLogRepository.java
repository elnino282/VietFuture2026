package org.example.season.repository;

import java.time.LocalDate;
import java.util.List;
import org.example.season.entity.FieldLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldLogRepository extends JpaRepository<FieldLog, Integer> {

    List<FieldLog> findAllBySeasonId(Integer seasonId);

    List<FieldLog> findAllBySeasonIdAndLogDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    List<FieldLog> findTop10BySeasonIdOrderByLogDateDescIdDesc(Integer seasonId);

    boolean existsBySeasonId(Integer seasonId);

    long countBySeasonIdAndLogTypeIgnoreCase(Integer seasonId, String logType);

    @Query(value = """
            SELECT f.* FROM field_logs f
            JOIN seasons s ON f.season_id = s.season_id
            JOIN farm_db.plots p ON s.plot_id = p.plot_id
            JOIN farm_db.farms farm ON p.farm_id = farm.farm_id
            WHERE farm.user_id = :ownerId
            ORDER BY f.created_at DESC, f.field_log_id DESC
            """, nativeQuery = true)
    List<FieldLog> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
