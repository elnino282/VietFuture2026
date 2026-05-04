package org.example.QuanLyMuaVu.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for updating a user's status (ACTIVE/INACTIVE/LOCKED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStatusUpdateRequest {

    @NotBlank(message = "KEY_INVALID")
    String status;
}

