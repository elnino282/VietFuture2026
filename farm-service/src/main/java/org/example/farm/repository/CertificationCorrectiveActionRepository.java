package org.example.farm.repository;

import org.example.farm.entity.CertificationCorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificationCorrectiveActionRepository extends JpaRepository<CertificationCorrectiveAction, Long> {
    List<CertificationCorrectiveAction> findByNonconformityId(Long nonconformityId);
    Optional<CertificationCorrectiveAction> findTopByNonconformityIdOrderByCreatedAtDesc(Long nonconformityId);
}

