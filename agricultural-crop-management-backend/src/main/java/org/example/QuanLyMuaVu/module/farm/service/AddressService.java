package org.example.QuanLyMuaVu.module.farm.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.dto.response.ProvinceResponse;
import org.example.QuanLyMuaVu.module.farm.dto.response.WardResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.mapper.AddressMapper;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for address lookup operations (provinces, wards).
 * All read operations are cacheable since administrative data rarely changes.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AddressService {

    ProvinceRepository provinceRepository;
    WardRepository wardRepository;
    AddressMapper addressMapper;

    public List<Province> findAllProvinceEntities() {
        return provinceRepository.findAll();
    }

    public List<Ward> findWardsByProvince(Integer provinceId) {
        return wardRepository.findByProvinceId(provinceId);
    }

    // ==================== PROVINCE OPERATIONS ====================

    /**
     * Get all provinces with optional keyword and type filter.
     *
     * @param keyword optional search keyword
     * @param type    optional type filter ("thanh-pho" for city, "tinh" for
     *                province)
     */
    @Cacheable(value = "provinces", key = "#keyword + '-' + #type")
    public List<ProvinceResponse> getAllProvinces(String keyword, String type) {
        List<Province> provinces;

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasType = type != null && !type.trim().isEmpty();

        if (hasKeyword && hasType) {
            provinces = provinceRepository.findByNameContainingIgnoreCaseAndType(keyword.trim(), type.trim());
        } else if (hasKeyword) {
            provinces = provinceRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else if (hasType) {
            provinces = provinceRepository.findByType(type.trim());
        } else {
            provinces = provinceRepository.findAll();
        }

        return provinces.stream()
                .map(addressMapper::toProvinceResponse)
                .toList();
    }

    /**
     * Get a single province by ID.
     */
    @Cacheable(value = "province", key = "#id")
    public ProvinceResponse getProvinceById(Integer id) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));
        return addressMapper.toProvinceResponse(province);
    }

    // ==================== WARD OPERATIONS ====================

    /**
     * Get all wards for a specific province with optional keyword search.
     */
    @Cacheable(value = "wards", key = "#provinceId + '-' + #keyword")
    public List<WardResponse> getWardsByProvinceId(Integer provinceId, String keyword) {
        // Verify province exists
        if (!provinceRepository.existsById(provinceId)) {
            throw new AppException(ErrorCode.PROVINCE_NOT_FOUND);
        }

        List<Ward> wards;
        if (keyword != null && !keyword.trim().isEmpty()) {
            wards = wardRepository.findByProvinceIdAndNameContainingIgnoreCase(provinceId, keyword.trim());
        } else {
            wards = wardRepository.findByProvinceId(provinceId);
        }

        return wards.stream()
                .map(addressMapper::toWardResponse)
                .toList();
    }

    /**
     * Get a single ward by ID.
     */
    @Cacheable(value = "ward", key = "#id")
    public WardResponse getWardById(Integer id) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND));
        return addressMapper.toWardResponse(ward);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get count of all address entities (for import statistics).
     */
    public AddressStats getStats() {
        return AddressStats.builder()
                .provinceCount(provinceRepository.count())
                .wardCount(wardRepository.count())
                .build();
    }

    /**
     * Statistics record for address data.
     */
    @lombok.Builder
    @lombok.Data
    public static class AddressStats {
        private long provinceCount;
        private long wardCount;
    }
}
