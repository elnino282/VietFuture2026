package org.example.season.repository;

import org.example.season.entity.DiseaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiseaseRecordRepository extends JpaRepository<DiseaseRecord, Integer>, JpaSpecificationExecutor<DiseaseRecord> {
}
