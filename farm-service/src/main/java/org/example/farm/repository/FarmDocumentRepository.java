package org.example.farm.repository;

import org.example.farm.entity.FarmDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FarmDocumentRepository extends JpaRepository<FarmDocument, Integer> {

    List<FarmDocument> findByFarmId(Integer farmId);

    Page<FarmDocument> findByFarmId(Integer farmId, Pageable pageable);

    List<FarmDocument> findByFarmIdAndDocumentType(Integer farmId, String documentType);

    // Tài liệu sắp hết hạn (trong 30 ngày tới)
    @Query("SELECT d FROM FarmDocument d WHERE d.farmId = :farmId " +
           "AND d.expiryDate IS NOT NULL " +
           "AND d.expiryDate BETWEEN :today AND :deadline " +
           "ORDER BY d.expiryDate ASC")
    List<FarmDocument> findExpiringDocuments(
        @Param("farmId") Integer farmId,
        @Param("today") LocalDate today,
        @Param("deadline") LocalDate deadline);

    long countByFarmIdAndVerificationStatus(Integer farmId, String status);
}
