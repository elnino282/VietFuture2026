package org.example.QuanLyMuaVu.module.farm.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {

    /**
     * Get all wards belonging to a specific province.
     */
    List<Ward> findByProvinceId(Integer provinceId);

    /**
     * Search wards by name within a specific province.
     */
    List<Ward> findByProvinceIdAndNameContainingIgnoreCase(Integer provinceId, String keyword);

    /**
     * Check if any wards exist for a given province.
     */
    boolean existsByProvinceId(Integer provinceId);

    /**
     * Delete all wards by province ID.
     */
    void deleteByProvinceId(Integer provinceId);
}
