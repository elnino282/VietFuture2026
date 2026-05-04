package org.example.QuanLyMuaVu.module.farm.mapper;

import org.example.QuanLyMuaVu.module.farm.dto.response.ProvinceResponse;
import org.example.QuanLyMuaVu.module.farm.dto.response.WardResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class AddressMapperManual implements AddressMapper {

    @Override
    public ProvinceResponse toProvinceResponse(Province province) {
        if (province == null) {
            return null;
        }
        return ProvinceResponse.builder()
                .id(province.getId())
                .name(province.getName())
                .slug(province.getSlug())
                .type(province.getType())
                .nameWithType(province.getNameWithType())
                .build();
    }

    @Override
    public WardResponse toWardResponse(Ward ward) {
        if (ward == null) {
            return null;
        }
        return WardResponse.builder()
                .id(ward.getId())
                .name(ward.getName())
                .slug(ward.getSlug())
                .type(ward.getType())
                .nameWithType(ward.getNameWithType())
                .provinceId(ward.getProvince() != null ? ward.getProvince().getId() : null)
                .build();
    }
}
