package org.example.QuanLyMuaVu.module.season.repository;


import java.time.LocalDate;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.season.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Integer> {

    Optional<PayrollRecord> findByIdAndSeason_Id(Integer id, Integer seasonId);

    Optional<PayrollRecord> findByIdAndEmployee_Id(Integer id, Long employeeId);

    Optional<PayrollRecord> findByEmployee_IdAndSeason_IdAndPeriodStartAndPeriodEnd(
            Long employeeId,
            Integer seasonId,
            LocalDate periodStart,
            LocalDate periodEnd);

    @Query("SELECT p FROM PayrollRecord p " +
            "WHERE p.season.id = :seasonId " +
            "AND (:employeeId IS NULL OR p.employee.id = :employeeId) " +
            "ORDER BY p.periodStart DESC, p.generatedAt DESC, p.id DESC")
    Page<PayrollRecord> findBySeasonAndEmployee(@Param("seasonId") Integer seasonId,
            @Param("employeeId") Long employeeId,
            Pageable pageable);

    @Query("SELECT p FROM PayrollRecord p " +
            "WHERE p.employee.id = :employeeId " +
            "AND (:seasonId IS NULL OR p.season.id = :seasonId) " +
            "ORDER BY p.periodStart DESC, p.generatedAt DESC, p.id DESC")
    Page<PayrollRecord> findByEmployeeAndSeason(@Param("employeeId") Long employeeId,
            @Param("seasonId") Integer seasonId,
            Pageable pageable);
}
