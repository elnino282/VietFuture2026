package org.example.QuanLyMuaVu.module.admin.dto.request;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for updating a user via Admin API.
 */
@Data
@Builder
public class AdminUserUpdateRequest {
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles;
    private String status;
}
