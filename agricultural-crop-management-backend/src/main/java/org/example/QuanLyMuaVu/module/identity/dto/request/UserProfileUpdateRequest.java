package org.example.QuanLyMuaVu.module.identity.dto.request;

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
public class UserProfileUpdateRequest {

    /**
     * New username for the current user. If null or blank, username will not be
     * changed.
     */
    String username;

    /**
     * New email for the current user. If null or blank, email will not be changed.
     */
    String email;

    /**
     * New full name for the current user. If null or blank, fullName will not be
     * changed.
     */
    String fullName;

    /**
     * New phone number for the current user. If null or blank, phone will not be
     * changed.
     */
    String phone;

    /**
     * Province ID for address update. If null, province will not be changed.
     */
    Long provinceId;

    /**
     * Ward ID for address update. If null, ward will not be changed.
     */
    Long wardId;
}
