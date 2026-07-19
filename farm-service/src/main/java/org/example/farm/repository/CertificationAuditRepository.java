package org.example.farm.repository;

import org.example.farm.entity.CertificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificationAuditRepository extends JpaRepository<CertificationAudit, Long> {
    List<CertificationAudit> findByRecordIdOrderByCreatedAtDesc(Integer recordId);
    List<CertificationAudit> findByRecordId(Integer recordId);
}

