package org.example.QuanLyMuaVu.module.inventory.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.inventory.entity.StockLocation;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Integer> {

    List<StockLocation> findAllByWarehouse(Warehouse warehouse);

    boolean existsByWarehouse_Id(Integer warehouseId);
}

