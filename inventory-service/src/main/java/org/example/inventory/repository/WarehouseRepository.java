package org.example.inventory.repository;

import java.util.List;
import org.example.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findAllByFarmId(Integer farmId);
    List<Warehouse> findAllByTypeIgnoreCase(String type);
    List<Warehouse> findByFarmIdIn(List<Integer> farmIds);
    List<Warehouse> findByFarmIdInAndTypeIgnoreCase(List<Integer> farmIds, String type);
    boolean existsByFarmIdAndNameIgnoreCaseAndTypeIgnoreCase(Integer farmId, String name, String type);
    boolean existsByFarmIdAndNameIgnoreCaseAndTypeIgnoreCaseAndIdNot(Integer farmId, String name, String type, Integer id);
}
