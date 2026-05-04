package org.example.QuanLyMuaVu.module.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.UserSummaryReportResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminUserReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST endpoints for user reporting.
 * Provides summary statistics about user accounts.
 */
@RestController
@RequestMapping("/api/v1/admin/reports/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Reports", description = "Admin endpoints for user account reporting")
public class AdminUserReportController {

    AdminUserReportService adminUserReportService;

    @Operation(summary = "Get user account summary", description = "Returns total number of user accounts, number of active accounts, and number of locked accounts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/summary")
    public ApiResponse<UserSummaryReportResponse> getUserSummaryReport() {
        UserSummaryReportResponse result = adminUserReportService.getUserSummary();
        return ApiResponse.success("USER_REPORT_SUMMARY_SUCCESS", result);
    }
}
