package org.example.farm.repository;

import java.util.List;
import org.example.farm.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {
    List<Province> findByNameContainingIgnoreCase(String keyword);
    List<Province> findByType(String type);
    List<Province> findByNameContainingIgnoreCaseAndType(String keyword, String type);
}
