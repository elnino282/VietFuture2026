package org.example.QuanLyMuaVu.module.admin.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminUserCreateRequest;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminUserStatusUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminUserUpdateRequest;
import org.example.QuanLyMuaVu.module.admin.service.AdminUserCommandService;
import org.example.QuanLyMuaVu.module.admin.service.AdminUserQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin User Controller
 * Provides full CRUD operations for user management (admin only).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

        private final AdminUserQueryService adminUserQueryService;
        private final AdminUserCommandService adminUserCommandService;

        /**
         * GET /api/v1/admin/users
         * Returns paginated list of all users
         */
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<AdminUserQueryService.AdminUserDto>>> getAllUsers(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String sortDir) {
                log.info("Admin requesting all users, page: {}, size: {}", page, size);

                Sort sort = sortDir.equalsIgnoreCase("desc")
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<AdminUserQueryService.AdminUserDto> usersPage = keyword != null && !keyword.isBlank()
                                ? adminUserQueryService.searchUsers(keyword, pageable)
                                : adminUserQueryService.getAllUsers(pageable);

                PageResponse<AdminUserQueryService.AdminUserDto> response = PageResponse.of(usersPage,
                                usersPage.getContent());
                return ResponseEntity.ok(ApiResponse.success("Users retrieved", response));
        }

        /**
         * GET /api/v1/admin/users/{id}
         * Get user by ID
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<AdminUserCommandService.AdminUserResponse>> getUserById(
                        @PathVariable Long id) {
                log.info("Admin requesting user by ID: {}", id);

                AdminUserCommandService.AdminUserResponse user = adminUserCommandService.getUserById(id);
                return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
        }

        /**
         * POST /api/v1/admin/users
         * Create a new user
         */
        @PostMapping
        public ResponseEntity<ApiResponse<AdminUserCommandService.AdminUserResponse>> createUser(
                        @Valid @RequestBody AdminUserCreateRequest request) {
                log.info("Admin creating new user: {}", request.getUsername());

                AdminUserCommandService.AdminUserResponse user = adminUserCommandService.createUser(request);
                return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
        }

        /**
         * PUT /api/v1/admin/users/{id}
         * Update an existing user
         */
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<AdminUserCommandService.AdminUserResponse>> updateUser(
                        @PathVariable Long id,
                        @Valid @RequestBody AdminUserUpdateRequest request) {
                log.info("Admin updating user ID: {}", id);

                AdminUserCommandService.AdminUserResponse user = adminUserCommandService.updateUser(id, request);
                return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
        }

        /**
         * PATCH /api/v1/admin/users/{id}/status
         * Update user status (ACTIVE, LOCKED, INACTIVE)
         */
        @PatchMapping("/{id}/status")
        public ResponseEntity<ApiResponse<AdminUserCommandService.AdminUserResponse>> updateStatus(
                        @PathVariable Long id,
                        @Valid @RequestBody AdminUserStatusUpdateRequest request) {
                log.info("Admin updating status for user ID: {} to {}", id, request.getStatus());

                AdminUserCommandService.AdminUserResponse user = adminUserCommandService.updateStatus(id,
                                request.getStatus());
                return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
        }

        /**
         * PATCH /api/v1/admin/users/{id}/password
         * Reset user password (admin operation)
         */
        @PatchMapping("/{id}/password")
        public ResponseEntity<ApiResponse<AdminUserCommandService.AdminUserResponse>> resetPassword(
                        @PathVariable Long id,
                        @RequestBody Map<String, String> request) {
                log.info("Admin resetting password for user ID: {}", id);

                String password = request.get("password");
                AdminUserCommandService.AdminUserResponse user = adminUserCommandService.resetPassword(id, password);
                return ResponseEntity.ok(ApiResponse.success("Password reset successfully", user));
        }

        /**
         * GET /api/v1/admin/users/{id}/can-delete
         * Check if a user can be deleted (no associated farms)
         */
        @GetMapping("/{id}/can-delete")
        public ResponseEntity<ApiResponse<Boolean>> canDelete(@PathVariable Long id) {
                log.info("Admin checking if user ID: {} can be deleted", id);

                boolean canDelete = adminUserCommandService.canDelete(id);
                return ResponseEntity.ok(ApiResponse.success("Delete check completed", canDelete));
        }

        /**
         * DELETE /api/v1/admin/users/{id}
         * Delete a user (throws error if user has associated farms)
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
                log.info("Admin deleting user ID: {}", id);

                adminUserCommandService.deleteUser(id);
                return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        }

        /**
         * GET /api/v1/admin/users/roles
         * Returns list of all available roles
         */
        @GetMapping("/roles")
        public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
                log.info("Admin requesting all roles");

                List<String> roles = adminUserQueryService.getAllRoles();

                return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roles));
        }
}
