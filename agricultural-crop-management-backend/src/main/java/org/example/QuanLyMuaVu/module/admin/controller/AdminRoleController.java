package org.example.QuanLyMuaVu.module.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.identity.dto.request.RoleRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.RoleResponse;
import org.example.QuanLyMuaVu.module.identity.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for role management.
 * Provides CRUD operations for user roles (ADMIN only).
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Roles", description = "Quản lý vai trò người dùng (chỉ ADMIN)")
public class AdminRoleController {

    RoleService roleService;

    @PostMapping
    @Operation(summary = "Tạo vai trò mới", description = "Tạo một vai trò mới trong hệ thống (chỉ ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo vai trò thành công", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền tạo vai trò"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ")
    })
    public ApiResponse<RoleResponse> createRole(
            @Parameter(description = "Thông tin vai trò cần tạo", required = true) @RequestBody RoleRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả vai trò", description = "Lấy danh sách tất cả vai trò trong hệ thống (chỉ ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền xem danh sách vai trò")
    })
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    @DeleteMapping("/{roleCode}")
    @Operation(summary = "Xóa vai trò", description = "Xóa một vai trò theo mã (chỉ ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa vai trò thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền xóa vai trò"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy vai trò")
    })
    public ApiResponse<Void> deleteRole(
            @Parameter(description = "Mã vai trò cần xóa", required = true) @PathVariable String roleCode) {
        roleService.deleteRoleByCode(roleCode);
        return ApiResponse.success(null);
    }
}
