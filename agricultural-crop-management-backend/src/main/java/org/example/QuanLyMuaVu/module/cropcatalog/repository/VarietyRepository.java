package org.example.QuanLyMuaVu.module.cropcatalog.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VarietyRepository extends JpaRepository<Variety, Integer> {

    List<Variety> findAllByCrop(Crop crop);
}


