package org.example.farm.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.farm.dto.response.ProvinceResponse;
import org.example.farm.dto.response.WardResponse;
import org.example.farm.entity.Province;
import org.example.farm.entity.Ward;
import org.example.farm.exception.AppException;
import org.example.farm.exception.ErrorCode;
import org.example.farm.repository.ProvinceRepository;
import org.example.farm.repository.WardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AddressService {

    ProvinceRepository provinceRepository;
    WardRepository wardRepository;

    public List<Province> findAllProvinceEntities() {
        return provinceRepository.findAll();
    }

    public List<Ward> findWardsByProvince(Integer provinceId) {
        return wardRepository.findByProvinceId(provinceId);
    }

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
                .map(this::toProvinceResponse)
                .toList();
    }

    @Cacheable(value = "province", key = "#id")
    public ProvinceResponse getProvinceById(Integer id) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));
        return toProvinceResponse(province);
    }

    @Cacheable(value = "wards", key = "#provinceId + '-' + #keyword")
    public List<WardResponse> getWardsByProvinceId(Integer provinceId, String keyword) {
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
                .map(this::toWardResponse)
                .toList();
    }

    @Cacheable(value = "ward", key = "#id")
    public WardResponse getWardById(Integer id) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND));
        return toWardResponse(ward);
    }

    public AddressStats getStats() {
        return AddressStats.builder()
                .provinceCount(provinceRepository.count())
                .wardCount(wardRepository.count())
                .build();
    }

    private ProvinceResponse toProvinceResponse(Province province) {
        if (province == null) return null;
        return ProvinceResponse.builder()
                .id(province.getId())
                .name(province.getName())
                .slug(province.getSlug())
                .type(province.getType())
                .nameWithType(province.getNameWithType())
                .build();
    }

    private WardResponse toWardResponse(Ward ward) {
        if (ward == null) return null;
        return WardResponse.builder()
                .id(ward.getId())
                .name(ward.getName())
                .slug(ward.getSlug())
                .type(ward.getType())
                .nameWithType(ward.getNameWithType())
                .provinceId(ward.getProvince() != null ? ward.getProvince().getId() : null)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class AddressStats {
        private long provinceCount;
        private long wardCount;
    }
}
