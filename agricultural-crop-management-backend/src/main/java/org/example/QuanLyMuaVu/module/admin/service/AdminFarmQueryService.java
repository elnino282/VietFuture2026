package org.example.QuanLyMuaVu.module.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin org.example.QuanLyMuaVu.module.farm.entity.Farm Query Service
 * Read-only queries for admin to view all farms across the system.
 * Uses existing FarmRepository and FarmResponse DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminFarmQueryService {

    private final FarmQueryPort farmQueryPort;

    /**
     * Get all farms with pagination (admin global view)
     * Uses LEFT JOIN FETCH to handle missing User references gracefully.
     */
    public Page<FarmResponse> getAllFarms(Pageable pageable) {
        log.info("Admin fetching all farms, page: {}", pageable.getPageNumber());

        Page<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms = farmQueryPort.findAllFarmsWithRelationships(pageable);

        return farms.map(this::toFarmResponse);
    }

    /**
     * Get all farms with optional keyword search
     * Uses LEFT JOIN FETCH to handle missing User references gracefully.
     */
    public Page<FarmResponse> searchFarms(String keyword, Pageable pageable) {
        log.info("Admin searching farms with keyword: {}", keyword);

        Page<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms;
        if (keyword != null && !keyword.isBlank()) {
            farms = farmQueryPort.searchFarmsWithRelationships(keyword, pageable);
        } else {
            farms = farmQueryPort.findAllFarmsWithRelationships(pageable);
        }

        return farms.map(this::toFarmResponse);
    }

    /**
     * Get farm count by status
     */
    public long countActiveFarms() {
        return farmQueryPort.countActiveFarms();
    }

    public long countInactiveFarms() {
        return farmQueryPort.countInactiveFarms();
    }

    /**
     * Map org.example.QuanLyMuaVu.module.farm.entity.Farm entity to FarmResponse DTO
     * Matches frontend expectations with both farmName and name fields,
     * plus ownerUsername.
     */
    private FarmResponse toFarmResponse(org.example.QuanLyMuaVu.module.farm.entity.Farm farm) {
        String ownerUsername = null;
        if (farm.getUser() != null) {
            ownerUsername = farm.getUser().getUsername();
        }

        return FarmResponse.builder()
                .id(farm.getId())
                .farmName(farm.getName())
                .name(farm.getName()) // Alias for frontend compatibility
                .ownerUsername(ownerUsername)
                .provinceId(farm.getProvince() != null ? farm.getProvince().getId() : null)
                .provinceName(farm.getProvince() != null ? farm.getProvince().getName() : null)
                .wardId(farm.getWard() != null ? farm.getWard().getId() : null)
                .wardName(farm.getWard() != null ? farm.getWard().getName() : null)
                .area(farm.getArea())
                .active(farm.getActive())
                .build();
    }
}
