package org.example.season.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.example.season.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Integer> {

    Optional<PayrollRecord> findByIdAndSeasonId(Integer id, Integer seasonId);

    Optional<PayrollRecord> findByIdAndEmployeeUserId(Integer id, Long employeeUserId);

    Optional<PayrollRecord> findByEmployeeUserIdAndSeasonIdAndPeriodStartAndPeriodEnd(
            Long employeeUserId,
            Integer seasonId,
            LocalDate periodStart,
            LocalDate periodEnd);

    @Query("SELECT p FROM PayrollRecord p " +
            "WHERE p.season.id = :seasonId " +
            "AND (:employeeUserId IS NULL OR p.employeeUserId = :employeeUserId) " +
            "ORDER BY p.periodStart DESC, p.generatedAt DESC, p.id DESC")
    Page<PayrollRecord> findBySeasonAndEmployee(@Param("seasonId") Integer seasonId,
            @Param("employeeUserId") Long employeeUserId,
            Pageable pageable);

    @Query("SELECT p FROM PayrollRecord p " +
            "WHERE p.employeeUserId = :employeeUserId " +
            "AND (:seasonId IS NULL OR p.season.id = :seasonId) " +
            "ORDER BY p.periodStart DESC, p.generatedAt DESC, p.id DESC")
    Page<PayrollRecord> findByEmployeeAndSeason(@Param("employeeUserId") Long employeeUserId,
            @Param("seasonId") Integer seasonId,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM PayrollRecord p WHERE p.season.id = :seasonId")
    BigDecimal sumTotalAmountBySeasonId(@Param("seasonId") Integer seasonId);
}
