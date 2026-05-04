package org.example.QuanLyMuaVu.module.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utility component for accessing current authenticated user information.
 * 
 * Provides convenient methods to:
 * - Get current user ID
 * - Get current user's primary role
 * - Get current org.example.QuanLyMuaVu.module.identity.entity.User entity
 * - Check if current user is farmer
 * 
 * Usage example:
 * 
 * <pre>
 * {@code
 * @Autowired
 * CurrentUserService currentUserService;
 * 
 * Long userId = currentUserService.getCurrentUserId();
 * String role = currentUserService.getCurrentRole();
 * org.example.QuanLyMuaVu.module.identity.entity.User user = currentUserService.getCurrentUser();
 * }
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final IdentityQueryPort identityQueryPort;

    /**
     * Get the current authenticated user's ID from JWT claims.
     * 
     * @return user ID
     * @throws AppException with UNAUTHENTICATED if no valid session
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
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

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * Get the current authenticated user's ID, or null if not authenticated.
     * Use this for optional authentication scenarios.
     * 
     * @return user ID or null
     */
    public Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (AppException e) {
            return null;
        }
    }

    /**
     * Get the current authenticated user's primary role from JWT claims.
     * 
     * @return role code (e.g., "FARMER", "BUYER")
     * @throws AppException with UNAUTHENTICATED if no valid session
     */
    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object roleClaim = jwt.getClaim("role");
            if (roleClaim instanceof String role) {
                return role;
            }
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * Get the current authenticated org.example.QuanLyMuaVu.module.identity.entity.User entity.
     * 
     * @return org.example.QuanLyMuaVu.module.identity.entity.User entity
     * @throws AppException with UNAUTHENTICATED if no valid session
     * @throws AppException with USER_NOT_FOUND if user doesn't exist
     */
    public org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser() {
        Long userId = getCurrentUserId();
        return identityQueryPort.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Check if current user has FARMER role.
     * 
     * @return true if current user is farmer
     */
    public boolean isFarmer() {
        try {
            return "FARMER".equalsIgnoreCase(getCurrentRole());
        } catch (AppException e) {
            return false;
        }
    }

    /**
     * Require that the current user is authenticated.
     * 
     * @throws AppException with UNAUTHENTICATED if no valid session
     */
    public void requireAuthenticated() {
        getCurrentUserId();
    }

    /**
     * Require that the current user has a specific role.
     * 
     * @param requiredRole the role code to check for
     * @throws AppException with UNAUTHENTICATED if no valid session
     * @throws AppException with FORBIDDEN if role doesn't match
     */
    public void requireRole(String requiredRole) {
        String currentRole = getCurrentRole();
        if (!requiredRole.equalsIgnoreCase(currentRole)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}
