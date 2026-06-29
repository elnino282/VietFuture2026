package org.example.adminreporting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.entity.UserSummary;
import org.example.adminreporting.repository.UserSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserSummaryRepository userSummaryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Admin requesting all users from reporting service, page: {}, size: {}", page, size);

        // Map sortBy field to entity field if needed
        String entitySortBy = sortBy;
        if ("id".equals(sortBy)) {
            entitySortBy = "userId";
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(entitySortBy).descending()
                : Sort.by(entitySortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserSummary> usersPage = keyword != null && !keyword.isBlank()
                ? userSummaryRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        keyword, keyword, keyword, pageable)
                : userSummaryRepository.findAll(pageable);

        Page<AdminUserDto> dtoPage = usersPage.map(this::toAdminUserDto);
        PageResponse<AdminUserDto> response = PageResponse.of(dtoPage, dtoPage.getContent());
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", response));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        log.info("Admin requesting all roles from reporting service");
        List<String> roles = userSummaryRepository.findDistinctRoleCodes();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roles));
    }

    private AdminUserDto toAdminUserDto(UserSummary user) {
        List<String> roleList = user.getRoleCode() != null ? List.of(user.getRoleCode()) : List.of();
        return new AdminUserDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                roleList
        );
    }

    public record AdminUserDto(
            Long id,
            String username,
            String email,
            String fullName,
            String status,
            List<String> roles) {
    }
}
