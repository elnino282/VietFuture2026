package org.example.QuanLyMuaVu.module.cropcatalog.repository;

import java.util.Optional;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.CropNitrogenReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CropNitrogenReferenceRepository extends JpaRepository<CropNitrogenReference, Integer> {

    Optional<CropNitrogenReference> findFirstByCrop_IdAndActiveTrue(Integer cropId);
}


