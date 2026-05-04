package org.example.QuanLyMuaVu.module.identity.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerCreationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.ResetPasswordRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.SignUpRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.UserProfileUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.mapper.FarmerMapper;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    FarmQueryPort farmQueryPort;
    FarmerMapper farmerMapper;
    PasswordEncoder passwordEncoder;

    public FarmerResponse signUp(SignUpRequest request) {
        String effectiveUsername = request.getEffectiveUsername();
        if (effectiveUsername == null || effectiveUsername.isBlank()) {
            throw new AppException(ErrorCode.USERNAME_BLANK);
        }

        FarmerCreationRequest creationRequest = FarmerCreationRequest.builder()
                .username(effectiveUsername)
                .password(request.getPassword())
                .build();

        User user = farmerMapper.toUser(creationRequest);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            email = effectiveUsername.contains("@") ? effectiveUsername : null;
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim());
        }

        String fullName = request.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }

        HashSet<Role> roles = new HashSet<>();

        String roleCode = request.getRole() != null
                ? request.getRole().trim().toUpperCase()
                : PredefinedRole.BUYER_ROLE;
        if (!PredefinedRole.BUYER_ROLE.equals(roleCode) && !PredefinedRole.FARMER_ROLE.equals(roleCode)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        roleRepository.findByCode(roleCode).ifPresent(roles::add);

        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        user.setRoles(roles);
        user.setStatus(UserStatus.ACTIVE);
        user.setJoinedDate(LocalDateTime.now());

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return farmerMapper.toFarmerResponse(user);
    }

    public FarmerResponse getMyInfo() {
        User user = resolveCurrentUser();

        return farmerMapper.toFarmerResponse(user);
    }

    public FarmerResponse updateProfile(UserProfileUpdateRequest request) {
        User user = resolveCurrentUser();

        // Update username if provided and different
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(request.getUsername());
        }

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail().trim());
        }

        // Update fullName if provided
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        // Update phone if provided
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }

        // Update province if provided
        if (request.getProvinceId() != null) {
            user.setProvince(farmQueryPort.findProvinceById(request.getProvinceId().intValue())
                    .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND)));
        }

        // Update ward if provided
        if (request.getWardId() != null) {
            user.setWard(farmQueryPort.findWardById(request.getWardId().intValue())
                    .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_FOUND)));
        }

        return farmerMapper.toFarmerResponse(userRepository.save(user));
    }

    public FarmerResponse changeMyPassword(FarmerUpdateRequest request) {
        User user = resolveCurrentUser();

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return farmerMapper.toFarmerResponse(userRepository.save(user));
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String identifier = authentication.getName();
        if (identifier == null || identifier.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        try {
            Long userId = Long.parseLong(identifier);
            return userRepository.findByIdWithRoles(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        } catch (NumberFormatException ex) {
            return userRepository.findByIdentifierWithRoles(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }
    }
}
