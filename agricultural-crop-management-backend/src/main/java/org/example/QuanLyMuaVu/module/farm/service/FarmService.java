package org.example.QuanLyMuaVu.module.farm.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmCreateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmUpdateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<FarmResponse> getMyFarms() {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        List<Farm> farms = farmRepository.findAllByUser(currentUser);
        return farms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FarmResponse getFarmDetail(Integer farmId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        Farm farm = farmRepository.findByIdAndUser(farmId, currentUser)
                .orElseThrow(() -> new AccessDeniedException(
                        "Access Denied: You do not own this farm or it does not exist."));
        return toResponse(farm);
    }

    @Transactional(readOnly = true)
    public PageResponse<FarmResponse> searchMyFarms(String keyword, Boolean active, int page, int size) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        String trimmedKeyword = keyword != null ? keyword.trim() : null;
        if (trimmedKeyword != null && trimmedKeyword.isEmpty()) {
            trimmedKeyword = null;
        }

        Page<Farm> farmsPage = farmRepository.searchByUserAndKeywordAndActive(
                currentUser,
                trimmedKeyword,
                active,
                PageRequest.of(page, size));

        List<FarmResponse> items = farmsPage.getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(farmsPage, items);
    }

    @Transactional
    public FarmResponse createFarm(FarmCreateRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();

        // Check if active farm with same name exists
        if (farmRepository.existsByUserAndNameIgnoreCaseAndActiveTrue(currentUser, request.getFarmName())) {
            throw new AppException(ErrorCode.FARM_NAME_EXISTS);
        }

        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));

        Ward ward = wardRepository.findById(request.getWardId())
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND));

        if (!ward.getProvince().getId().equals(province.getId())) {
            throw new AppException(ErrorCode.WARD_NOT_IN_PROVINCE);
        }

        Farm farm = Farm.builder()
                .name(request.getFarmName())
                .area(request.getArea())
                .active(request.getActive())
                .user(currentUser)
                .province(province)
                .ward(ward)
                .build();

        Farm savedFarm = farmRepository.save(farm);
        return toResponse(savedFarm);
    }

    @Transactional
    public FarmResponse updateFarm(Integer farmId, FarmUpdateRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        Farm farm = farmRepository.findByIdAndUser(farmId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (farmRepository.existsByUserAndNameIgnoreCaseAndActiveTrueAndIdNot(
                    currentUser,
                    trimmedName,
                    farm.getId())) {
                throw new AppException(ErrorCode.FARM_NAME_EXISTS);
            }
            farm.setName(trimmedName);
        }

        Province province = farm.getProvince();
        Ward ward = farm.getWard();

        if (request.getProvinceId() != null) {
            province = provinceRepository.findById(request.getProvinceId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND));
        }

        if (request.getWardId() != null) {
            ward = wardRepository.findById(request.getWardId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND));
        }

        if (province != null && ward != null && !ward.getProvince().getId().equals(province.getId())) {
            throw new AppException(ErrorCode.WARD_NOT_IN_PROVINCE);
        }

        farm.setProvince(province);
        farm.setWard(ward);

        if (request.getArea() != null) {
            farm.setArea(request.getArea());
        }

        if (request.getActive() != null) {
            farm.setActive(request.getActive());
        }

        Farm savedFarm = farmRepository.save(farm);
        return toResponse(savedFarm);
    }

    @Transactional
    public void deleteFarm(Integer farmId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        Farm farm = farmRepository.findByIdAndUser(farmId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        if (Boolean.FALSE.equals(farm.getActive())) {
            throw new AppException(ErrorCode.FARM_ALREADY_INACTIVE);
        }

        if (farmRepository.hasPlots(farmId) || farmRepository.hasSeasons(farmId)) {
            throw new AppException(ErrorCode.FARM_HAS_CHILD_RECORDS);
        }

        farm.setActive(false);
        farmRepository.save(farm);
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private FarmResponse toResponse(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .farmName(farm.getName())
                .provinceId(farm.getProvince().getId())
                .provinceName(farm.getProvince().getName())
                .wardId(farm.getWard().getId())
                .wardName(farm.getWard().getName())
                .area(farm.getArea())
                .active(farm.getActive())
                .build();
    }
}
