package org.example.QuanLyMuaVu.module.farm.mapper;

import org.example.QuanLyMuaVu.module.farm.dto.response.ProvinceResponse;
import org.example.QuanLyMuaVu.module.farm.dto.response.WardResponse;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for address entities (Province, Ward).
 */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    /**
     * Map Province entity to ProvinceResponse DTO.
     */
    ProvinceResponse toProvinceResponse(Province province);

    /**
     * Map Ward entity to WardResponse DTO.
     */
    @Mapping(target = "provinceId", source = "province.id")
    WardResponse toWardResponse(Ward ward);
}
