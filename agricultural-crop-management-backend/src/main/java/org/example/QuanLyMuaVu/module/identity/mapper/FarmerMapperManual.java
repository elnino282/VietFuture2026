package org.example.QuanLyMuaVu.module.identity.mapper;

import java.util.List;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerCreationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FarmerMapperManual implements FarmerMapper {

    @Override
    public User toUser(FarmerCreationRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
    }

    @Override
    public FarmerResponse toFarmerResponse(User user) {
        if (user == null) {
            return null;
        }

        List<String> roleCodes = List.of();
        if (user.getRoles() != null) {
            roleCodes = user.getRoles().stream()
                    .filter(role -> role != null && role.getCode() != null)
                    .map(Role::getCode)
                    .toList();
        }

        return FarmerResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus() != null ? user.getStatus().getCode() : null)
                .roles(roleCodes)
                .build();
    }

    @Override
    public void updateUser(User user, FarmerUpdateRequest request) {
        if (user == null || request == null) {
            return;
        }
        user.setPassword(request.getPassword());
    }
}
