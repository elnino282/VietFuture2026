package org.example.QuanLyMuaVu.module.farm.mapper;

import org.example.QuanLyMuaVu.module.farm.dto.request.FarmCreateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmUpdateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmDetailResponse;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper {

    public Farm toEntity(FarmCreateRequest request) {
        if (request == null) {
            return null;
        }
        Province province = null;
        if (request.getProvinceId() != null) {
            province = Province.builder().id(request.getProvinceId()).build();
        }
        Ward ward = null;
        if (request.getWardId() != null) {
            ward = Ward.builder().id(request.getWardId()).build();
        }
        return Farm.builder()
                .name(request.getFarmName())
                .area(request.getArea())
                .province(province)
                .ward(ward)
                .active(true)
                .build();
    }

    public void updateEntity(Farm farm, FarmUpdateRequest request) {
        if (farm == null || request == null) {
            return;
        }
        if (request.getName() != null) {
            farm.setName(request.getName());
        }
        if (request.getArea() != null) {
            farm.setArea(request.getArea());
        }
        if (request.getActive() != null) {
            farm.setActive(request.getActive());
        }
        if (request.getProvinceId() != null) {
            farm.setProvince(Province.builder().id(request.getProvinceId()).build());
        }
        if (request.getWardId() != null) {
            farm.setWard(Ward.builder().id(request.getWardId()).build());
        }
    }

    public FarmResponse toResponse(Farm farm) {
        if (farm == null) {
            return null;
        }
        return FarmResponse.builder()
                .id(farm.getId())
                .farmName(farm.getName())
                .area(farm.getArea())
                .active(farm.getActive())
                .build();
    }

    public FarmDetailResponse toDetailResponse(Farm farm) {
        if (farm == null) {
            return null;
        }
        return FarmDetailResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .provinceId(farm.getProvince() != null ? farm.getProvince().getId() : null)
                .wardId(farm.getWard() != null ? farm.getWard().getId() : null)
                .provinceName(farm.getProvince() != null ? farm.getProvince().getName() : null)
                .wardName(farm.getWard() != null ? farm.getWard().getName() : null)
                .area(farm.getArea())
                .active(farm.getActive())
                .ownerUsername(farm.getUser() != null ? farm.getUser().getUsername() : null)
                .build();
    }
}
