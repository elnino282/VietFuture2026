package org.example.QuanLyMuaVu.module.admin.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminUserUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentFavoriteRepository;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentRecentOpenRepository;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.port.IdentityCommandPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserCommandServiceTest {

    @Mock
    private IdentityCommandPort identityCommandPort;

    @Mock
    private FarmQueryPort farmQueryPort;

    @Mock
    private DocumentFavoriteRepository documentFavoriteRepository;

    @Mock
    private DocumentRecentOpenRepository documentRecentOpenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminUserCommandService adminUserCommandService;

    @Test
    void updateStatus_statusChanged_logsAuditEntry() {
        User user = User.builder()
                .id(12L)
                .username("target-user")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().code("FARMER").build()))
                .build();

        User actor = User.builder()
                .id(99L)
                .username("admin-user")
                .build();

        when(identityCommandPort.findUserById(12L)).thenReturn(Optional.of(user));
        when(identityCommandPort.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserService.getCurrentUser()).thenReturn(actor);

        adminUserCommandService.updateStatus(12L, "LOCKED");

        verify(auditLogService).logModuleOperation(
                eq("IDENTITY"),
                eq("USER"),
                eq(12),
                eq("RBAC_STATUS_UPDATED"),
                eq("admin-user"),
                any(),
                eq("Admin updated user status"),
                eq(null));
    }

    @Test
    void updateUser_rolesChanged_logsRoleAuditOnly() {
        User user = User.builder()
                .id(15L)
                .username("target-user")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().code("FARMER").build()))
                .build();

        Role adminRole = Role.builder().code("ADMIN").build();
        Role farmerRole = Role.builder().code("FARMER").build();
        User actor = User.builder()
                .id(100L)
                .username("admin-root")
                .build();

        AdminUserUpdateRequest request = AdminUserUpdateRequest.builder()
                .roles(Set.of("ADMIN", "FARMER"))
                .build();

        when(identityCommandPort.findUserById(15L)).thenReturn(Optional.of(user));
        when(identityCommandPort.findRolesByCodes(any())).thenReturn(List.of(adminRole, farmerRole));
        when(identityCommandPort.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserService.getCurrentUser()).thenReturn(actor);

        adminUserCommandService.updateUser(15L, request);

        verify(auditLogService).logModuleOperation(
                eq("IDENTITY"),
                eq("USER"),
                eq(15),
                eq("RBAC_ROLE_UPDATED"),
                eq("admin-root"),
                any(),
                eq("Admin updated user roles"),
                eq(null));
        verify(auditLogService, never()).logModuleOperation(
                eq("IDENTITY"),
                eq("USER"),
                eq(15),
                eq("RBAC_STATUS_UPDATED"),
                any(),
                any(),
                any(),
                any());
    }
}
