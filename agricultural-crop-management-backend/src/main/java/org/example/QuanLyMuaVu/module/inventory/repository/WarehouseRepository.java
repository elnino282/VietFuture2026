package org.example.QuanLyMuaVu.module.inventory.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

    List<Warehouse> findAllByFarm(org.example.QuanLyMuaVu.module.farm.entity.Farm farm);

    List<Warehouse> findByFarmIn(List<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms);

    List<Warehouse> findByFarmInAndTypeIgnoreCase(List<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms, String type);

    boolean existsByFarmAndNameIgnoreCaseAndTypeIgnoreCase(org.example.QuanLyMuaVu.module.farm.entity.Farm farm, String name, String type);

    boolean existsByFarmAndNameIgnoreCaseAndTypeIgnoreCaseAndIdNot(
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm,
            String name,
            String type,
            Integer id);
}
