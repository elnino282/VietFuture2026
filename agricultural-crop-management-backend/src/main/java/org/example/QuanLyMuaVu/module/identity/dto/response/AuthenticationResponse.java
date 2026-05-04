package org.example.QuanLyMuaVu.module.identity.dto.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Authentication response containing JWT token, user profile, and redirect
 * information.
 * Used for both sign-in and /me endpoint responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    /**
     * JWT access token
     */
    String token;

    @Builder.Default
    String tokenType = "Bearer";

    /**
     * Token expiration in seconds
     */
    Long expiresIn;

    /**
     * User ID
     */
    Long userId;

    /**
     * User's email address
     */
    String email;

    /**
     * Username for backwards compatibility
     */
    String username;

    /**
     * List of role codes assigned to the user
     */
    List<String> roles;

    /**
     * Primary role for authorization (BUYER/FARMER)
     */
    String role;

    /**
     * User profile information
     */
    ProfileInfo profile;

    /**
     * Redirect URL based on role ("/buyer" or "/farmer")
     */
    String redirectTo;

    /**
     * Nested class for user profile details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProfileInfo {
        Long id;
        String fullName;
        String email;
        String phone;
        String status;
        String joinedDate;
        Integer provinceId;
        Integer wardId;
    }
}
