package org.example.QuanLyMuaVu.module.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Config.FarmSecurityProperties;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.Util.RequestUtils;
import org.example.QuanLyMuaVu.module.identity.dto.request.ForgotPasswordRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.ResetPasswordTokenRequest;
import org.example.QuanLyMuaVu.module.identity.service.PasswordResetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetController {

    PasswordResetService passwordResetService;
    FarmSecurityProperties farmSecurityProperties;

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request,
                                            HttpServletRequest httpServletRequest) {
        String email = request != null ? request.getEmail() : null;
        String ip = RequestUtils.resolveClientIp(httpServletRequest, farmSecurityProperties);
        String userAgent = RequestUtils.resolveUserAgent(httpServletRequest);
        passwordResetService.requestPasswordReset(email, ip, userAgent);
        return ApiResponse.success(
                "If that email exists, we sent a reset link. Please check your inbox.",
                null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordTokenRequest request,
                                           HttpServletRequest httpServletRequest) {
        String ip = RequestUtils.resolveClientIp(httpServletRequest, farmSecurityProperties);
        passwordResetService.resetPassword(
                request != null ? request.getToken() : null,
                request != null ? request.getNewPassword() : null,
                request != null ? request.getConfirmPassword() : null,
                ip);
        return ApiResponse.success("Password reset successful. Please sign in again.", null);
    }

    @GetMapping("/reset-password/validate")
    public ApiResponse<Void> validateResetToken(@RequestParam("token") String token) {
        passwordResetService.validateToken(token);
        return ApiResponse.success(null);
    }
}
