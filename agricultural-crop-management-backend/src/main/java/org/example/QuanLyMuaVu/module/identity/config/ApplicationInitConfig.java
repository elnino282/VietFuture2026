package org.example.QuanLyMuaVu.module.identity.config;

import java.util.HashSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    // Admin account constants
    static final String ADMIN_USER_NAME = "admin";
    static final String ADMIN_EMAIL = "admin@acm.local";
    static final String ADMIN_PASSWORD = "admin123";
    static final String ADMIN_FULL_NAME = "Administrator";
    static final String ADMIN_PHONE = "0900000000";

    // Farmer account constants
    static final String FARMER_USER_NAME = "farmer";
    static final String FARMER_EMAIL = "farmer@acm.local";
    static final String FARMER_PASSWORD = "12345678";
    static final String FARMER_FULL_NAME = "Nguyen Van Farmer";
    static final String FARMER_PHONE = "0901234567";

    // Employee account constants
    static final String EMPLOYEE_USER_NAME = "employee";
    static final String EMPLOYEE_EMAIL = "employee@acm.local";
    static final String EMPLOYEE_PASSWORD = "12345678";
    static final String EMPLOYEE_FULL_NAME = "Nguyen Van Employee";
    static final String EMPLOYEE_PHONE = "0902234567";

    // Buyer account constants
    static final String BUYER_USER_NAME = "buyer";
    static final String BUYER_EMAIL = "buyer@acm.local";
    static final String BUYER_PASSWORD = "12345678";
    static final String BUYER_FULL_NAME = "Tran Thi Buyer";
    static final String BUYER_PHONE = "0903234000";

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Dang khoi tao du lieu mac dinh (vai tro va nguoi dung)...");
        return args -> {
            // 1. Ensure default roles exist
            org.example.QuanLyMuaVu.module.identity.entity.Role adminRole = ensureRoleExists(
                    PredefinedRole.ADMIN_ROLE,
                    "Admin",
                    "Administrator user with full access",
                    roleRepository);
            org.example.QuanLyMuaVu.module.identity.entity.Role farmerRole = ensureRoleExists(
                    PredefinedRole.FARMER_ROLE,
                    "Farmer",
                    "Farmer user",
                    roleRepository);
            org.example.QuanLyMuaVu.module.identity.entity.Role employeeRole = ensureRoleExists(
                    PredefinedRole.EMPLOYEE_ROLE,
                    "Employee",
                    "Employee user",
                    roleRepository);
            org.example.QuanLyMuaVu.module.identity.entity.Role buyerRole = ensureRoleExists(
                    PredefinedRole.BUYER_ROLE,
                    "Buyer",
                    "Buyer user",
                    roleRepository);

            // 2. Ensure default admin user exists with role
            ensureUserExistsWithRole(ADMIN_USER_NAME, ADMIN_EMAIL, ADMIN_PASSWORD,
                    ADMIN_FULL_NAME, ADMIN_PHONE, adminRole, userRepository);

            // 3. Ensure default farmer user exists with role
            ensureUserExistsWithRole(FARMER_USER_NAME, FARMER_EMAIL, FARMER_PASSWORD,
                    FARMER_FULL_NAME, FARMER_PHONE, farmerRole, userRepository);

            // 4. Ensure default employee user exists with role
            ensureUserExistsWithRole(EMPLOYEE_USER_NAME, EMPLOYEE_EMAIL, EMPLOYEE_PASSWORD,
                    EMPLOYEE_FULL_NAME, EMPLOYEE_PHONE, employeeRole, userRepository);

            // 5. Ensure default buyer user exists with role
            ensureUserExistsWithRole(BUYER_USER_NAME, BUYER_EMAIL, BUYER_PASSWORD,
                    BUYER_FULL_NAME, BUYER_PHONE, buyerRole, userRepository);

            log.info("Khoi tao du lieu mac dinh hoan tat.");
        };
    }

    private org.example.QuanLyMuaVu.module.identity.entity.Role ensureRoleExists(String code, String name, String description, RoleRepository roleRepository) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    log.info("Creating default role with code: {}", code);
                    org.example.QuanLyMuaVu.module.identity.entity.Role role = org.example.QuanLyMuaVu.module.identity.entity.Role.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .build();
                    return roleRepository.save(role);
                });
    }

    private void ensureUserExistsWithRole(String username, String email, String password,
            String fullName, String phone, org.example.QuanLyMuaVu.module.identity.entity.Role role,
            UserRepository userRepository) {
        Optional<org.example.QuanLyMuaVu.module.identity.entity.User> existingUserByEmail = userRepository.findByEmailIgnoreCaseWithRoles(email);
        Optional<org.example.QuanLyMuaVu.module.identity.entity.User> existingUserByUsername = userRepository.findByUsernameWithRoles(username);

        if (existingUserByEmail.isPresent()) {
            org.example.QuanLyMuaVu.module.identity.entity.User user = existingUserByEmail.get();
            boolean needsUpdate = false;
            if (user.getRoles() == null || user.getRoles().isEmpty() ||
                    user.getRoles().stream().noneMatch(r -> r.getCode().equals(role.getCode()))) {
                log.info("Adding missing role {} to user: {}", role.getCode(), email);
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(role);
                needsUpdate = true;
            }
            if (needsUpdate) {
                userRepository.save(user);
            } else {
                log.info("org.example.QuanLyMuaVu.module.identity.entity.User already exists with correct role: {}", email);
            }
            return;
        }

        if (existingUserByUsername.isPresent()) {
            org.example.QuanLyMuaVu.module.identity.entity.User user = existingUserByUsername.get();
            log.info("org.example.QuanLyMuaVu.module.identity.entity.User found by username '{}' but with different email. Updating user details.", username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setStatus(UserStatus.ACTIVE);
            
            if (user.getRoles() == null || user.getRoles().isEmpty() ||
                    user.getRoles().stream().noneMatch(r -> r.getCode().equals(role.getCode()))) {
                log.info("Adding missing role {} to user: {}", role.getCode(), email);
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(role);
            }
            userRepository.save(user);
            return;
        }

        var roles = new HashSet<org.example.QuanLyMuaVu.module.identity.entity.Role>();
        roles.add(role);

        org.example.QuanLyMuaVu.module.identity.entity.User newUser = org.example.QuanLyMuaVu.module.identity.entity.User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        userRepository.save(newUser);
        log.info("Default user created: {} / {}", email, password);
    }
}
