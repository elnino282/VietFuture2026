package org.example.QuanLyMuaVu.module.season.repository;

import java.time.LocalDate;
import java.util.List;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldLogRepository extends JpaRepository<FieldLog, Integer> {

    List<FieldLog> findAllBySeason_Id(Integer seasonId);

    List<FieldLog> findAllBySeason_IdAndLogDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    List<FieldLog> findTop10BySeason_IdOrderByLogDateDescIdDesc(Integer seasonId);

    boolean existsBySeason_Id(Integer seasonId);

    long countBySeason_IdAndLogTypeIgnoreCase(Integer seasonId, String logType);

    @Query("""
            SELECT f FROM FieldLog f
            LEFT JOIN FETCH f.season s
            LEFT JOIN FETCH s.plot p
            WHERE s.plot.farm.user.id = :ownerId
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    List<FieldLog> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
