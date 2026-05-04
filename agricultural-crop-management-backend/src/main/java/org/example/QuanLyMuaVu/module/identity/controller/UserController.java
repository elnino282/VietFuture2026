package org.example.QuanLyMuaVu.module.identity.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.identity.dto.request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.UserProfileUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.FarmerResponse;
import org.example.QuanLyMuaVu.module.identity.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @GetMapping("/me")
    public ApiResponse<FarmerResponse> me() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @PutMapping("/profile")
    public ApiResponse<FarmerResponse> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @PutMapping("/change-password")
    public ApiResponse<FarmerResponse> changePassword(@Valid @RequestBody FarmerUpdateRequest request) {
        return ApiResponse.success(userService.changeMyPassword(request));
    }
}
