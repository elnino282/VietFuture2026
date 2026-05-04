package org.example.QuanLyMuaVu.module.admin.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin org.example.QuanLyMuaVu.module.identity.entity.User Query Service
 * Queries for admin to view and manage users across the system.
 * Uses IdentityQueryPort and RoleRepository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserQueryService {

    private final IdentityQueryPort identityQueryPort;

    /**
     * DTO for admin user list view
     */
    public record AdminUserDto(
            Long id,
            String username,
            String email,
            String fullName,
            String status,
            List<String> roles) {
    }

    /**
     * Get all users with pagination
     */
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        log.info("Admin fetching all users, page: {}", pageable.getPageNumber());

        Page<org.example.QuanLyMuaVu.module.identity.entity.User> users = identityQueryPort.findAllUsers(pageable);

        return users.map(this::toAdminUserDto);
    }

    /**
     * Search users by keyword (username, email, or fullName)
     */
    public Page<AdminUserDto> searchUsers(String keyword, Pageable pageable) {
        log.info("Admin searching users with keyword: {}", keyword);

        Page<org.example.QuanLyMuaVu.module.identity.entity.User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = identityQueryPort.searchUsersByKeyword(keyword, pageable);
        } else {
            users = identityQueryPort.findAllUsers(pageable);
        }

        return users.map(this::toAdminUserDto);
    }

    /**
     * Get all available roles
     */
    public List<String> getAllRoles() {
        return identityQueryPort.findAllRoleCodes();
    }

    /**
     * Count users by status
     */
    public long countByStatus(UserStatus status) {
        return identityQueryPort.countUsersByStatus(status);
    }

    private AdminUserDto toAdminUserDto(org.example.QuanLyMuaVu.module.identity.entity.User user) {
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toList())
                : List.of();

        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus() != null ? user.getStatus().name() : null,
                roleNames);
    }
}
