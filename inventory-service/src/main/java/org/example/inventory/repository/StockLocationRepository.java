package org.example.inventory.repository;

import java.util.List;
import org.example.inventory.entity.StockLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Integer> {
    List<StockLocation> findAllByWarehouseId(Integer warehouseId);
    boolean existsByWarehouseId(Integer warehouseId);
}
