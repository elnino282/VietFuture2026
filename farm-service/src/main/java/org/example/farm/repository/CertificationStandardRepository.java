package org.example.farm.repository;

import org.example.farm.entity.CertificationStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificationStandardRepository extends JpaRepository<CertificationStandard, Integer> {
    Optional<CertificationStandard> findByCode(String code);
}
