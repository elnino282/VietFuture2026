package org.example.QuanLyMuaVu.module.farm.port;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmQueryPort {

    Optional<Farm> findFarmById(Integer farmId);

    List<Farm> findFarmsByIds(List<Integer> farmIds);

    List<Farm> findFarmsByOwnerId(Long ownerId);

    List<Farm> findAllFarms();

    long countFarms();

    long countActiveFarmsByOwnerId(Long ownerId);

    Optional<Plot> findPlotById(Integer plotId);

    List<Plot> findPlotsByOwnerId(Long ownerId);

    List<Plot> findPlotsByOwnerIdAndFarmId(Long ownerId, Integer farmId);

    List<Plot> findPlotsByFarmId(Integer farmId);

    List<Plot> findAllPlots();

    long countPlots();

    long countPlotsByOwnerId(Long ownerId);

    Optional<Province> findProvinceById(Integer provinceId);

    Optional<Ward> findWardById(Integer wardId);

    Page<Farm> findAllFarmsWithRelationships(Pageable pageable);

    Page<Farm> searchFarmsWithRelationships(String keyword, Pageable pageable);

    long countActiveFarms();

    long countInactiveFarms();

    boolean existsFarmByOwnerId(Long ownerId);
}
