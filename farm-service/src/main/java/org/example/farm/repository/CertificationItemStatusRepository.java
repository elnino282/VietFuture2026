package org.example.farm.repository;

import org.example.farm.entity.CertificationItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificationItemStatusRepository extends JpaRepository<CertificationItemStatus, Integer> {
    List<CertificationItemStatus> findByRecordId(Integer recordId);
    Optional<CertificationItemStatus> findByRecordIdAndChecklistItemId(Integer recordId, Integer checklistItemId);
}
