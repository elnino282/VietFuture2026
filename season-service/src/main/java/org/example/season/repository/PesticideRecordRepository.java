package org.example.season.repository;

import org.example.season.entity.PesticideRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

public interface PesticideRecordRepository extends JpaRepository<PesticideRecord, Integer> {

    List<PesticideRecord> findBySeasonId(Integer seasonId);

    Optional<PesticideRecord> findByFieldLogId(Integer fieldLogId);

    // Tìm các pesticide có harvest_allowed_date trong tương lai (chưa hết cách ly)
    @Query("SELECT p FROM PesticideRecord p WHERE p.seasonId = :seasonId " +
           "AND p.harvestAllowedDate > :checkDate " +
           "ORDER BY p.harvestAllowedDate ASC")
    List<PesticideRecord> findActivePHIBySeason(
        @Param("seasonId") Integer seasonId,
        @Param("checkDate") LocalDate checkDate);

    // Tìm vi phạm PHI — harvest trước ngày cho phép
    @Query("SELECT p FROM PesticideRecord p WHERE p.seasonId = :seasonId " +
           "AND :harvestDate < p.harvestAllowedDate")
    List<PesticideRecord> findPHIViolations(
        @Param("seasonId") Integer seasonId,
        @Param("harvestDate") LocalDate harvestDate);
}
