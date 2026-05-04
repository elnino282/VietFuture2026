package org.example.QuanLyMuaVu.module.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for creating a new user via Admin API.
 */
@Data
@Builder
public class AdminUserCreateRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles;
}
