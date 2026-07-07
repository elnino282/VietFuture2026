package org.example.farm.repository;

import org.example.farm.entity.CertificationChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificationChecklistItemRepository extends JpaRepository<CertificationChecklistItem, Integer> {
    List<CertificationChecklistItem> findByStandardId(Integer standardId);
}
