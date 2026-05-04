package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.identity.entity.PasswordResetToken;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.repository.PasswordResetTokenRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.identity.service.PasswordResetRateLimiter;
import org.example.QuanLyMuaVu.module.identity.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    PasswordResetTokenRepository tokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    EmailSender emailSender;

    @Mock
    PasswordResetRateLimiter rateLimiter;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordEncoder,
                emailSender,
                rateLimiter);
        ReflectionTestUtils.setField(passwordResetService, "appUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(passwordResetService, "expiryMinutes", 15L);
    }

    @Test
    void validateToken_withValidToken_doesNotThrow() {
        String rawToken = "valid-token";
        String hash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(User.builder().id(99L).status(UserStatus.ACTIVE).build())
                .tokenHash(hash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(eq(hash), any()))
                .thenReturn(Optional.of(resetToken));

        passwordResetService.validateToken(rawToken);
    }

    @Test
    void resetPassword_withValidToken_updatesPasswordAndMarksUsed() {
        String rawToken = "reset-token";
        String hash = hashToken(rawToken);

        User user = User.builder()
                .id(1L)
                .status(UserStatus.ACTIVE)
                .password("old")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(10L)
                .user(user)
                .tokenHash(hash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(eq(hash), any()))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.resetPassword(rawToken, "newpassword", "newpassword", "127.0.0.1");

        assertEquals("encoded", user.getPassword());
        assertNotNull(resetToken.getUsedAt());
    }

    @Test
    void resetPassword_withInvalidToken_throwsAppException() {
        String rawToken = "invalid-token";
        String hash = hashToken(rawToken);

        when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(eq(hash), any()))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> passwordResetService.resetPassword(rawToken, "newpassword", "newpassword", "127.0.0.1"));

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, exception.getErrorCode());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
