package org.example.season.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.season.entity.DiseaseTreatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiseaseTreatmentRepository extends JpaRepository<DiseaseTreatment, Integer> {

    Page<DiseaseTreatment> findAllByDiseaseRecord_Id(Integer diseaseRecordId, Pageable pageable);

    List<DiseaseTreatment> findAllByDiseaseRecord_IdOrderByTreatedAtDescIdDesc(Integer diseaseRecordId);

    long countByDiseaseRecord_Id(Integer diseaseRecordId);

    @Query("SELECT MAX(t.treatedAt) FROM DiseaseTreatment t WHERE t.diseaseRecord.id = :diseaseRecordId")
    LocalDateTime findLatestTreatedAtByDiseaseRecordId(@Param("diseaseRecordId") Integer diseaseRecordId);

    @Query("SELECT COALESCE(SUM(t.costAmount), 0) FROM DiseaseTreatment t WHERE t.diseaseRecord.id = :diseaseRecordId")
    BigDecimal sumCostAmountByDiseaseRecordId(@Param("diseaseRecordId") Integer diseaseRecordId);

    @Query("""
            SELECT t FROM DiseaseTreatment t
            WHERE t.diseaseRecord.season.id = :seasonId
            """)
    List<DiseaseTreatment> findAllBySeasonIdWithExpense(@Param("seasonId") Integer seasonId);

    void deleteAllByDiseaseRecord_Id(Integer diseaseRecordId);

    default void deleteAllByDiseaseRecordId(Integer diseaseRecordId) {
        deleteAllByDiseaseRecord_Id(diseaseRecordId);
    }

    default Page<DiseaseTreatment> findAllByDiseaseRecordId(Integer diseaseRecordId, Pageable pageable) {
        return findAllByDiseaseRecord_Id(diseaseRecordId, pageable);
    }

    default List<DiseaseTreatment> findAllByDiseaseRecordIdOrderByTreatedAtDescIdDesc(Integer diseaseRecordId) {
        return findAllByDiseaseRecord_IdOrderByTreatedAtDescIdDesc(diseaseRecordId);
    }

    default long countByDiseaseRecordId(Integer diseaseRecordId) {
        return countByDiseaseRecord_Id(diseaseRecordId);
    }
}


