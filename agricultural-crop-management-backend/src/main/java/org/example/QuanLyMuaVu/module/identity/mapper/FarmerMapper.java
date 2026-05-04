package org.example.QuanLyMuaVu.module.identity.mapper;

import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerCreationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FarmerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "province", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "joinedDate", ignore = true)
    User toUser(FarmerCreationRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "status", source = "status.code")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "provinceName", source = "province.name")
    @Mapping(target = "wardId", source = "ward.id")
    @Mapping(target = "wardName", source = "ward.name")
    @Mapping(target = "joinedDate", source = "joinedDate")
    FarmerResponse toFarmerResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "province", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "joinedDate", ignore = true)
    void updateUser(@MappingTarget User user, FarmerUpdateRequest request);
}
