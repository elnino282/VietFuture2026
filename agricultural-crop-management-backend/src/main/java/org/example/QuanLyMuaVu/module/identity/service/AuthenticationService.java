package org.example.QuanLyMuaVu.module.identity.service;

import com.nimbusds.jose.*;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.dto.request.AuthenticationRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.IntrospectRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.LogoutRequest;
import org.example.QuanLyMuaVu.module.identity.dto.request.RefreshRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.AuthenticationResponse;
import org.example.QuanLyMuaVu.module.identity.dto.response.IntrospectResponse;
import org.example.QuanLyMuaVu.module.identity.entity.InvalidatedToken;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.InvalidatedTokenRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Authentication service handling login, logout, token refresh, and
 * introspection.
 * Refactored to follow Single Responsibility Principle.
 * 
 * Responsibilities split into:
 * - AuthenticationService (this): Authentication flow only
 * - JwtTokenService: Token generation and verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenService jwtTokenService;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        log.debug("Introspecting token: {}", token.substring(0, Math.min(20, token.length())) + "...");

        boolean isValid = true;

        try {
            jwtTokenService.verifyToken(token, false);
            log.debug("Token introspection successful - token is valid");
        } catch (AppException e) {
            log.warn("Token introspection failed: {}", e.getMessage());
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    /**
     * Authenticate user by identifier (email OR username) and password.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String identifier = request.getEffectiveIdentifier();
        if (identifier == null || identifier.isBlank()) {
            log.warn("Authentication failed - no identifier provided");
            throw new AppException(ErrorCode.IDENTIFIER_REQUIRED);
        }

        log.info("Authentication attempt for identifier: {}", identifier);

        User user = userRepository
                .findByIdentifierWithRoles(identifier)
                .orElseThrow(() -> {
                    log.warn("Authentication failed - identifier not found: {}", identifier);
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {
            log.warn("Authentication failed - invalid password for identifier: {}", identifier);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Check user status and return appropriate error
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Authentication failed - user not active. Identifier: {}, Status: {}",
                    identifier, user.getStatus());
            
            // Return specific error based on status
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new AppException(ErrorCode.USER_LOCKED);
            } else if (user.getStatus() == UserStatus.INACTIVE) {
                throw new AppException(ErrorCode.USER_INACTIVE);
            } else {
                throw new AppException(ErrorCode.USER_LOCKED);
            }
        }

        if (CollectionUtils.isEmpty(user.getRoles())) {
            log.warn("Authentication failed - no roles assigned to user: {}", identifier);
            throw new AppException(ErrorCode.ROLE_MISSING);
        }

        String primaryRole = determinePrimaryRole(user);
        var token = jwtTokenService.generateToken(user, primaryRole);
        log.info("Authentication successful for identifier: {} - token generated, role: {}",
                identifier, primaryRole);

        return buildAuthResponse(user, primaryRole, token);
    }

    /**
     * Get current user info (for /api/v1/auth/me endpoint).
     */
    public AuthenticationResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user = userRepository.findByIdentifierWithRoles(user.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String primaryRole = determinePrimaryRole(user);
        return buildAuthResponse(user, primaryRole, null);
    }

    /**
     * Get the current authenticated user's ID from JWT claims.
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object userIdClaim = jwt.getClaim("user_id");
            if (userIdClaim instanceof Number num) {
                return num.longValue();
            }
            if (userIdClaim instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse user_id from JWT: {}", str);
                }
            }
        }
        return null;
    }

    /**
     * Get the current authenticated user's primary role from JWT claims.
     */
    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object roleClaim = jwt.getClaim("role");
            if (roleClaim instanceof String role) {
                return role;
            }
        }
        return null;
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        log.info("Logout attempt for token: {}",
                request.getToken().substring(0, Math.min(20, request.getToken().length())) + "...");

        try {
            var signToken = jwtTokenService.verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Token invalidated successfully - JIT: {}", jit);
        } catch (AppException exception) {
            log.info("Logout - Token already expired or invalid");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        log.info("Token refresh attempt");

        var signedJWT = jwtTokenService.verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);
        log.debug("Old token invalidated - JIT: {}", jit);

        var email = signedJWT.getJWTClaimsSet().getClaim("email");
        String identifier = email != null ? email.toString() : signedJWT.getJWTClaimsSet().getSubject();
        log.debug("Refreshing token for identifier: {}", identifier);

        var user = userRepository.findByIdentifierWithRoles(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String primaryRole = determinePrimaryRole(user);
        var token = jwtTokenService.generateToken(user, primaryRole);
        log.info("Token refreshed successfully for identifier: {}", identifier);

        return buildAuthResponse(user, primaryRole, token);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Determine primary role for a user.
     * Business rule: prefer FARMER over BUYER, otherwise take first role.
     */
    private String determinePrimaryRole(User user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .toList();

        // ADMIN has highest priority
        if (roleCodes.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roleCodes.contains("FARMER")) {
            return "FARMER";
        }
        if (roleCodes.contains("EMPLOYEE")) {
            return "EMPLOYEE";
        }
        if (roleCodes.contains("BUYER")) {
            return "BUYER";
        }
        return roleCodes.isEmpty() ? null : roleCodes.get(0);
    }

    /**
     * Determine redirect path based on role.
     */
    private String determineRedirectPath(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return "/admin";
        }
        if ("FARMER".equalsIgnoreCase(role)) {
            return "/farmer";
        }
        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            return "/employee";
        }
        if ("BUYER".equalsIgnoreCase(role)) {
            return "/marketplace";
        }
        return "/";
    }

    /**
     * Build authentication response.
     */
    private AuthenticationResponse buildAuthResponse(User user, String primaryRole, String token) {
        AuthenticationResponse.ProfileInfo profile = AuthenticationResponse.ProfileInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .joinedDate(user.getJoinedDate() != null ? user.getJoinedDate().toString() : null)
                .provinceId(user.getProvince() != null ? user.getProvince().getId() : null)
                .wardId(user.getWard() != null ? user.getWard().getId() : null)
                .build();

        var builder = AuthenticationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getCode).toList())
                .role(primaryRole)
                .profile(profile)
                .redirectTo(determineRedirectPath(primaryRole));

        if (token != null) {
            builder.token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenService.getValidDuration());
        }

        return builder.build();
    }
}
