package org.example.QuanLyMuaVu.module.admin.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.admin.dto.response.UserSummaryReportResponse;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserReportService {

    IdentityQueryPort identityQueryPort;

    public UserSummaryReportResponse getUserSummary() {
        long totalUsers = identityQueryPort.countUsers();
        long activeUsers = identityQueryPort.countUsersByStatus(UserStatus.ACTIVE);
        long lockedUsers = identityQueryPort.countUsersByStatus(UserStatus.LOCKED);

        return UserSummaryReportResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .build();
    }
}

