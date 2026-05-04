package org.example.QuanLyMuaVu.module.season.repository;

import java.time.LocalDate;
import java.util.List;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldLogRepository extends JpaRepository<FieldLog, Integer> {

    List<FieldLog> findAllBySeason_Id(Integer seasonId);

    List<FieldLog> findAllBySeason_IdAndLogDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    boolean existsBySeason_Id(Integer seasonId);

    long countBySeason_IdAndLogTypeIgnoreCase(Integer seasonId, String logType);
}
