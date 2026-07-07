package org.example.farm.repository;

import org.example.farm.entity.CertificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificationRecordRepository extends JpaRepository<CertificationRecord, Integer> {
    List<CertificationRecord> findByFarmId(Integer farmId);
    Optional<CertificationRecord> findByFarmIdAndStandardId(Integer farmId, Integer standardId);
}
