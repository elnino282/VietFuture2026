package org.example.QuanLyMuaVu.module.identity.mapper;

import org.example.QuanLyMuaVu.module.identity.dto.request.RoleRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.RoleResponse;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    Role toRole(RoleRequest request);

    @Mapping(source = "id", target = "id")
    RoleResponse toRoleResponse(Role role);
}
