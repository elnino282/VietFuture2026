package org.example.farm.repository;

import org.example.farm.entity.CertificationNonconformity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificationNonconformityRepository extends JpaRepository<CertificationNonconformity, Long> {
    List<CertificationNonconformity> findByAuditId(Long auditId);
    List<CertificationNonconformity> findByAuditIdIn(List<Long> auditIds);
}

