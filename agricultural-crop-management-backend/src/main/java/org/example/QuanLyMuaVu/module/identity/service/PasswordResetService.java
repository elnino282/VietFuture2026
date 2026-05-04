package org.example.QuanLyMuaVu.module.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Service.EmailSender;
import org.example.QuanLyMuaVu.module.identity.entity.PasswordResetToken;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.PasswordResetTokenRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PasswordResetService {

    final PasswordResetTokenRepository tokenRepository;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final EmailSender emailSender;
    final PasswordResetRateLimiter rateLimiter;

    @Value("${app.url:http://localhost:5173}")
    String appUrl;

    @Value("${app.password-reset.expiry-minutes:15}")
    long expiryMinutes;

    final SecureRandom secureRandom = new SecureRandom();

    public void requestPasswordReset(String email, String ip, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();
        log.info("event=forgot_password_requested requestId={} email={} ip={}", requestId, safeEmail(email), ip);
        try {
            if (!rateLimiter.isAllowed(email, ip)) {
                log.warn("event=forgot_password_rate_limited requestId={} email={} ip={}",
                        requestId, safeEmail(email), ip);
                return;
            }

            if (email == null || email.isBlank()) {
                return;
            }

            Optional<User> userOptional = userRepository.findByEmailIgnoreCaseWithRoles(email.trim());
            if (userOptional.isEmpty()) {
                return;
            }

            User user = userOptional.get();
            if (user.getStatus() != UserStatus.ACTIVE || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }

            String rawToken = generateToken();
            String tokenHash = hashToken(rawToken);
            LocalDateTime now = LocalDateTime.now();

            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(expiryMinutes))
                    .requestIp(ip)
                    .userAgent(trimUserAgent(userAgent))
                    .build();

            PasswordResetToken savedToken = tokenRepository.save(token);
            log.info("event=token_saved tokenId={} tokenHashPrefix={} expiresAt={} userId={}",
                    savedToken.getId(), tokenHash.substring(0, 16) + "...", savedToken.getExpiresAt(), user.getId());

            String resetLink = UriComponentsBuilder.fromHttpUrl(appUrl)
                    .path("/reset-password")
                    .queryParam("token", rawToken)
                    .build()
                    .toUriString();
            log.info("event=reset_link_generated rawTokenPrefix={} resetLink={}",
                    rawToken.substring(0, Math.min(10, rawToken.length())) + "...", resetLink);

            String htmlBody = buildResetEmail(resetLink, expiryMinutes);
            try {
                log.info("event=forgot_password_email_attempt requestId={} email={} ip={}",
                        requestId, safeEmail(email), ip);
                emailSender.sendHtml(user.getEmail(), "Reset your password", htmlBody);
                log.info("event=forgot_password_email_sent requestId={} email={} ip={}",
                        requestId, safeEmail(email), ip);
            } catch (Exception ex) {
                log.error("event=forgot_password_email_failed requestId={} email={} error={}",
                        requestId, safeEmail(email), ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            log.error("event=forgot_password_failed requestId={} email={} error={}",
                    requestId, safeEmail(email), ex.getMessage(), ex);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword, String ip) {
        String requestId = java.util.UUID.randomUUID().toString();
        try {
            PasswordResetToken resetToken = requireValidToken(token);
            User user = resetToken.getUser();

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.USER_LOCKED);
            }

            if (newPassword == null || newPassword.length() < 8) {
                throw new AppException(ErrorCode.PASSWORD_INVALID);
            }
            if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
                throw new AppException(ErrorCode.PASSWORD_MISMATCH);
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            resetToken.setUsedAt(LocalDateTime.now());
            tokenRepository.save(resetToken);

            log.info("event=reset_password_succeeded requestId={} userId={} ip={}",
                    requestId, user.getId(), ip);
        } catch (AppException ex) {
            log.warn("event=reset_password_failed requestId={} errorCode={} ip={}",
                    requestId, ex.getErrorCode().getCode(), ip);
            throw ex;
        }
    }

    public void validateToken(String token) {
        requireValidToken(token);
    }

    private PasswordResetToken requireValidToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("event=token_validation_failed reason=token_empty");
            throw new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        String hash = hashToken(token);
        LocalDateTime now = LocalDateTime.now();
        log.debug("event=token_validation_attempt tokenHash={} now={}", hash.substring(0, 16) + "...", now);

        Optional<PasswordResetToken> tokenOpt = tokenRepository
                .findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hash, now);

        if (tokenOpt.isEmpty()) {
            // Enhanced debug: check if token exists at all to diagnose the issue
            Optional<PasswordResetToken> existingToken = tokenRepository.findFirstByTokenHash(hash);
            if (existingToken.isPresent()) {
                PasswordResetToken t = existingToken.get();
                if (t.getUsedAt() != null) {
                    log.warn("event=token_validation_failed reason=already_used tokenHash={} usedAt={}",
                            hash.substring(0, 16) + "...", t.getUsedAt());
                } else if (t.getExpiresAt().isBefore(now)) {
                    log.warn("event=token_validation_failed reason=expired tokenHash={} expiresAt={} now={}",
                            hash.substring(0, 16) + "...", t.getExpiresAt(), now);
                }
            } else {
                log.warn("event=token_validation_failed reason=not_found tokenHash={}",
                        hash.substring(0, 16) + "...");
            }
        }

        return tokenOpt.orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Token hashing failed", ex);
        }
    }

    private String trimUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String trimmed = userAgent.trim();
        return trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed;
    }

    private String buildResetEmail(String resetLink, long expiresInMinutes) {
        return """
                <div style="background:#f5f7f2;padding:40px 0;font-family:'Segoe UI',Arial,sans-serif;color:#2b3674;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:16px;padding:36px 40px;box-shadow:0 8px 24px rgba(17,24,39,0.08);text-align:center;">
                    <h1 style="margin:0 0 12px;font-size:28px;line-height:1.3;font-weight:700;color:#22326b;">Reset Your Password</h1>
                    <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#6b7a99;">
                      We received a request to reset your password. Click the button below to create a new password.
                    </p>
                    <a href="%s" style="display:inline-block;background:#3ba55d;color:#ffffff;text-decoration:none;padding:12px 28px;border-radius:12px;font-weight:600;font-size:14px;">
                      Reset Password
                    </a>
                    <div style="margin:28px 0 18px;height:1px;background:#e6ecf5;"></div>
                    <p style="margin:0 0 12px;font-size:13px;color:#6b7a99;">
                      Warning: This link will expire in %d minutes.
                    </p>
                    <p style="margin:0 0 16px;font-size:13px;color:#6b7a99;">
                      If you didn't request this password reset, please ignore this email.
                    </p>
                    <p style="margin:0;font-size:12px;color:#94a3b8;">- The ACM Platform Team</p>
                    <p style="margin:18px 0 0;font-size:12px;color:#94a3b8;">
                      If the button does not work, copy and paste this link into your browser:
                    </p>
                    <p style="margin:6px 0 0;font-size:12px;word-break:break-all;">
                      <a href="%s" style="color:#3ba55d;text-decoration:none;">%s</a>
                    </p>
                  </div>
                </div>
                """
                .formatted(resetLink, expiresInMinutes, resetLink, resetLink);
    }

    private String safeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
