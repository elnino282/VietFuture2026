package org.example.QuanLyMuaVu.module.identity.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.identity.dto.request.RoleRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.RoleResponse;
import org.example.QuanLyMuaVu.module.identity.mapper.RoleMapper;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    public RoleResponse createRole(RoleRequest request) {
        var role = roleMapper.toRole(request);

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    public void deleteRoleByCode(String roleCode) {
        roleRepository.deleteByCode(roleCode);
    }
}
