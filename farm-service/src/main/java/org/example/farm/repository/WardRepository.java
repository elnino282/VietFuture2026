package org.example.farm.repository;

import java.util.List;
import org.example.farm.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {
    List<Ward> findByProvinceId(Integer provinceId);
    List<Ward> findByProvinceIdAndNameContainingIgnoreCase(Integer provinceId, String keyword);
    boolean existsByProvinceId(Integer provinceId);
    void deleteByProvinceId(Integer provinceId);
}
