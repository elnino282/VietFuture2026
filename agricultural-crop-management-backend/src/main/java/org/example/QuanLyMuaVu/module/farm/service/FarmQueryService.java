package org.example.QuanLyMuaVu.module.farm.service;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.farm.repository.PlotRepository;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class FarmQueryService implements FarmQueryPort {

    FarmRepository farmRepository;
    PlotRepository plotRepository;
    ProvinceRepository provinceRepository;
    WardRepository wardRepository;

    @Override
    public Optional<Farm> findFarmById(Integer farmId) {
        if (farmId == null) {
            return Optional.empty();
        }
        return farmRepository.findById(farmId);
    }

    @Override
    public List<Farm> findFarmsByIds(List<Integer> farmIds) {
        if (farmIds == null || farmIds.isEmpty()) {
            return List.of();
        }
        return farmRepository.findAllById(farmIds);
    }

    @Override
    public List<Farm> findFarmsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return farmRepository.findAllByUser_Id(ownerId);
    }

    @Override
    public List<Farm> findAllFarms() {
        return farmRepository.findAll();
    }

    @Override
    public long countFarms() {
        return farmRepository.count();
    }

    @Override
    public long countActiveFarmsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }
        return farmRepository.countByUserIdAndActiveTrue(ownerId);
    }

    @Override
    public Optional<Plot> findPlotById(Integer plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return plotRepository.findById(plotId);
    }

    @Override
    public List<Plot> findPlotsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        return plotRepository.findAllByFarmUserId(ownerId);
    }

    @Override
    public List<Plot> findPlotsByOwnerIdAndFarmId(Long ownerId, Integer farmId) {
        if (ownerId == null || farmId == null) {
            return List.of();
        }
        return plotRepository.findAllByFarmUserIdAndFarmId(ownerId, farmId);
    }

    @Override
    public List<Plot> findPlotsByFarmId(Integer farmId) {
        if (farmId == null) {
            return List.of();
        }
        return plotRepository.findAllByFarm_Id(farmId);
    }

    @Override
    public List<Plot> findAllPlots() {
        return plotRepository.findAll();
    }

    @Override
    public long countPlots() {
        return plotRepository.count();
    }

    @Override
    public long countPlotsByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }
        return plotRepository.countByFarmUserId(ownerId);
    }

    @Override
    public Optional<Province> findProvinceById(Integer provinceId) {
        if (provinceId == null) {
            return Optional.empty();
        }
        return provinceRepository.findById(provinceId);
    }

    @Override
    public Optional<Ward> findWardById(Integer wardId) {
        if (wardId == null) {
            return Optional.empty();
        }
        return wardRepository.findById(wardId);
    }

    @Override
    public Page<Farm> findAllFarmsWithRelationships(Pageable pageable) {
        if (pageable == null) {
            return Page.empty();
        }
        return farmRepository.findAllWithRelationships(pageable);
    }

    @Override
    public Page<Farm> searchFarmsWithRelationships(String keyword, Pageable pageable) {
        if (pageable == null) {
            return Page.empty();
        }
        if (keyword == null || keyword.isBlank()) {
            return farmRepository.findAllWithRelationships(pageable);
        }
        return farmRepository.findByNameContainingIgnoreCaseWithRelationships(keyword, pageable);
    }

    @Override
    public long countActiveFarms() {
        return farmRepository.countByIsActiveTrue();
    }

    @Override
    public long countInactiveFarms() {
        return farmRepository.countByIsActiveFalse();
    }

    @Override
    public boolean existsFarmByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return false;
        }
        return farmRepository.existsByUserId(ownerId);
    }
}
