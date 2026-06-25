package org.example.identity.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.identity.entity.User;
import org.example.identity.enums.UserStatus;
import org.example.identity.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/users/{id}")
    public ResponseEntity<UserInternalDto> getUserInternal(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        UserInternalDto dto = UserInternalDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/users/employees")
    public ResponseEntity<Page<UserInternalDto>> searchEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.searchByRoleAndStatusAndKeyword(
                "EMPLOYEE",
                UserStatus.ACTIVE,
                keyword,
                pageable);

        Page<UserInternalDto> dtoPage = users.map(user -> UserInternalDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .build());

        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/users/employees/{id}/validate")
    public ResponseEntity<Boolean> validateEmployee(@PathVariable Long id) {
        boolean exists = userRepository.existsByIdAndRoleCode(id, "EMPLOYEE");
        if (!exists) {
            return ResponseEntity.ok(false);
        }
        User user = userRepository.findById(id).orElse(null);
        boolean active = user != null && user.getStatus() == UserStatus.ACTIVE;
        return ResponseEntity.ok(active);
    }

    @Data
    @Builder
    public static class UserInternalDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phone;
    }
}
