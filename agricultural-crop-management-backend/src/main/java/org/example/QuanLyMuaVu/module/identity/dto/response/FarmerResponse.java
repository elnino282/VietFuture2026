package org.example.QuanLyMuaVu.module.identity.dto.response;

import java.time.LocalDateTime;
import java.util.List;
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
public class FarmerResponse {

    Long id;
    String username;
    String email;
    String fullName;
    String phone;
    String status;
    List<String> roles;

    // Address fields
    Long provinceId;
    String provinceName;
    Long wardId;
    String wardName;

    // Timestamp
    LocalDateTime joinedDate;
}
