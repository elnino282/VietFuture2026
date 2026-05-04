package org.example.QuanLyMuaVu.module.identity.service;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class IdentityQueryService implements IdentityQueryPort {

    UserRepository userRepository;
    RoleRepository roleRepository;

    @Override
    public Optional<User> findUserById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsersByKeyword(String keyword, Pageable pageable) {
        return userRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public boolean existsUserByIdAndRoleCode(Long userId, String roleCode) {
        if (userId == null || roleCode == null || roleCode.isBlank()) {
            return false;
        }
        return userRepository.existsByIdAndRoleCode(userId, roleCode);
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countUsersByStatus(UserStatus status) {
        if (status == null) {
            return 0L;
        }
        return userRepository.countByStatus(status);
    }

    @Override
    public Page<User> searchUsersByRoleAndStatusAndKeyword(String roleCode, UserStatus status, String keyword, Pageable pageable) {
        return userRepository.searchByRoleAndStatusAndKeyword(roleCode, status, keyword, pageable);
    }

    @Override
    public List<String> findAllRoleCodes() {
        return roleRepository.findAll().stream()
                .map(role -> role.getCode())
                .toList();
    }
}
