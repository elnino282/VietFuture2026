package org.example.QuanLyMuaVu.module.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Authentication request for sign-in.
 * Supports login via either username or email using the 'identifier' field.
 * The 'email' field is kept for backward compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    /**
     * Can be either username or email. Takes precedence over 'email' field.
     * If null, falls back to 'email' field.
     */
    String identifier;

    /**
     * Email address for login. Used for backward compatibility when 'identifier' is
     * not provided.
     */
    @Email
    String email;

    @NotBlank(message = "Password is required")
    String password;

    @Builder.Default
    Boolean rememberMe = false;

    /**
     * Returns the effective identifier for login.
     * Prefers 'identifier' field, falls back to 'email' if not provided.
     */
    public String getEffectiveIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return null;
    }
}
