package org.example.QuanLyMuaVu.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignUpRequest {

    @NotBlank(message = "USERNAME_BLANK")
    String username;

    /**
     * Optional email for account creation.
     * If not provided, email will be derived from username when possible.
     */
    String email;

    /**
     * Optional profile full name.
     */
    String fullName;

    /**
     * Optional phone number.
     */
    String phone;

    @NotBlank(message = "PASSWORD_BLANK")
    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;

    /**
     * Expected values: BUYER or FARMER.
     */
    @NotBlank
    String role;

    public String getEffectiveUsername() {
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return null;
    }
}
